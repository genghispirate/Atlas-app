package com.pact.app.core

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Calendar

/**
 * Single source of truth for the app's persisted state. Backed by
 * SharedPreferences (offline, instant), exposed to Compose as a StateFlow
 * snapshot, and safe to read from the AccessibilityService on every event
 * because reads hit the in-memory snapshot.
 */
class PactState private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("pact_state", Context.MODE_PRIVATE)

    data class Snapshot(
        val setupComplete: Boolean = false,
        val guardianName: String = "",
        val blocked: Set<String> = emptySet(),
        val unlockUntil: Map<String, Long> = emptyMap(),
        val failedAttempts: Int = 0,
        val lockoutUntil: Long = 0L,
        val strictMode: Boolean = false,
    )

    private val _snapshot = MutableStateFlow(read())
    val snapshot: StateFlow<Snapshot> = _snapshot

    sealed class VerifyResult {
        data object Ok : VerifyResult()
        data object Wrong : VerifyResult()
        /** Too many wrong attempts; locked until [untilMillis]. */
        data class TooManyAttempts(val untilMillis: Long) : VerifyResult()
    }

    // ---------------------------------------------------------------- setup

    fun completeSetup(guardianName: String, secretBase32: String, blocked: Set<String>) {
        prefs.edit()
            .putBoolean(KEY_SETUP, true)
            .putString(KEY_GUARDIAN, guardianName.trim())
            .putString(KEY_SECRET, Vault.encrypt(secretBase32))
            .putStringSet(KEY_BLOCKED, blocked)
            .putLong(KEY_LAST_STEP, Totp.stepAt(System.currentTimeMillis()))
            .apply()
        refresh()
    }

    /** Replace the guardian (new secret). Caller must have verified a current code first. */
    fun rePair(guardianName: String, newSecretBase32: String) {
        prefs.edit()
            .putString(KEY_GUARDIAN, guardianName.trim())
            .putString(KEY_SECRET, Vault.encrypt(newSecretBase32))
            .putLong(KEY_LAST_STEP, Totp.stepAt(System.currentTimeMillis()))
            .putInt(KEY_FAILED, 0)
            .putLong(KEY_LOCKOUT, 0L)
            .apply()
        refresh()
    }

    /** Full reset back to onboarding. Caller must have verified a current code first. */
    fun reset() {
        prefs.edit().clear().apply()
        refresh()
    }

    // ------------------------------------------------------------- blocking

    fun addBlocked(packages: Collection<String>) {
        val next = _snapshot.value.blocked + packages
        prefs.edit().putStringSet(KEY_BLOCKED, next).apply()
        refresh()
    }

    /** Removing protection requires a verified code — enforced by callers via [verifyCode]. */
    fun removeBlocked(pkg: String) {
        val next = _snapshot.value.blocked - pkg
        val unlocks = _snapshot.value.unlockUntil - pkg
        prefs.edit()
            .putStringSet(KEY_BLOCKED, next)
            .putString(KEY_UNLOCKS, encodeUnlocks(unlocks))
            .apply()
        refresh()
    }

    fun setStrictMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_STRICT, enabled).apply()
        refresh()
    }

    /**
     * Is [pkg] blocked right now? Called from the AccessibilityService for
     * every window change — reads only the in-memory snapshot.
     */
    fun isBlockedNow(pkg: String, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val s = _snapshot.value
        if (!s.setupComplete) return false
        val covered = s.blocked.contains(pkg) ||
            (s.strictMode && pkg == SETTINGS_PACKAGE)
        if (!covered) return false
        return (s.unlockUntil[pkg] ?: 0L) <= nowMillis
    }

    // -------------------------------------------------------------- unlocks

    fun unlockFor(pkg: String, durationMillis: Long) {
        val until = System.currentTimeMillis() + durationMillis
        val unlocks = _snapshot.value.unlockUntil + (pkg to until)
        prefs.edit().putString(KEY_UNLOCKS, encodeUnlocks(unlocks)).apply()
        refresh()
    }

    /** Ending a break early is always free — friction is one-directional. */
    fun relock(pkg: String) {
        val unlocks = _snapshot.value.unlockUntil - pkg
        prefs.edit().putString(KEY_UNLOCKS, encodeUnlocks(unlocks)).apply()
        refresh()
    }

    fun relockAll() {
        prefs.edit().putString(KEY_UNLOCKS, "").apply()
        refresh()
    }

    // ---------------------------------------------------------- code checks

    /**
     * Verify a guardian code. Applies rate limiting (5 wrong attempts →
     * 5-minute cooldown) and replay protection (each time step can only be
     * used once), so the 6-digit space cannot be brute-forced or reused.
     */
    fun verifyCode(code: String, nowMillis: Long = System.currentTimeMillis()): VerifyResult {
        val lockout = prefs.getLong(KEY_LOCKOUT, 0L)
        if (lockout > nowMillis) return VerifyResult.TooManyAttempts(lockout)

        val secret = prefs.getString(KEY_SECRET, null)?.let(Vault::decrypt)
            ?: return VerifyResult.Wrong

        val matchedStep = Totp.verify(secret, code, nowMillis)
        val lastStep = prefs.getLong(KEY_LAST_STEP, 0L)

        if (matchedStep == null || matchedStep <= lastStep) {
            val failed = prefs.getInt(KEY_FAILED, 0) + 1
            val edit = prefs.edit().putInt(KEY_FAILED, failed)
            if (failed >= MAX_ATTEMPTS) {
                val until = nowMillis + LOCKOUT_MILLIS
                edit.putLong(KEY_LOCKOUT, until).putInt(KEY_FAILED, 0)
                edit.apply()
                refresh()
                return VerifyResult.TooManyAttempts(until)
            }
            edit.apply()
            refresh()
            return VerifyResult.Wrong
        }

        prefs.edit()
            .putLong(KEY_LAST_STEP, matchedStep)
            .putInt(KEY_FAILED, 0)
            .putLong(KEY_LOCKOUT, 0L)
            .apply()
        refresh()
        return VerifyResult.Ok
    }

    // ------------------------------------------------------------- plumbing

    private fun refresh() {
        _snapshot.value = read()
    }

    private fun read(): Snapshot = Snapshot(
        setupComplete = prefs.getBoolean(KEY_SETUP, false),
        guardianName = prefs.getString(KEY_GUARDIAN, "") ?: "",
        blocked = prefs.getStringSet(KEY_BLOCKED, emptySet()) ?: emptySet(),
        unlockUntil = decodeUnlocks(prefs.getString(KEY_UNLOCKS, "") ?: ""),
        failedAttempts = prefs.getInt(KEY_FAILED, 0),
        lockoutUntil = prefs.getLong(KEY_LOCKOUT, 0L),
        strictMode = prefs.getBoolean(KEY_STRICT, false),
    )

    private fun encodeUnlocks(map: Map<String, Long>): String {
        val now = System.currentTimeMillis()
        return map.entries
            .filter { it.value > now }
            .joinToString(";") { "${it.key}=${it.value}" }
    }

    private fun decodeUnlocks(raw: String): Map<String, Long> =
        raw.split(";")
            .mapNotNull { entry ->
                val i = entry.lastIndexOf('=')
                if (i <= 0) null
                else entry.substring(0, i) to (entry.substring(i + 1).toLongOrNull() ?: return@mapNotNull null)
            }
            .toMap()

    companion object {
        const val SETTINGS_PACKAGE = "com.android.settings"
        const val MAX_ATTEMPTS = 5
        const val LOCKOUT_MILLIS = 5 * 60 * 1000L

        private const val KEY_SETUP = "setup_complete"
        private const val KEY_GUARDIAN = "guardian_name"
        private const val KEY_SECRET = "secret_blob"
        private const val KEY_BLOCKED = "blocked_packages"
        private const val KEY_UNLOCKS = "unlock_until"
        private const val KEY_FAILED = "failed_attempts"
        private const val KEY_LOCKOUT = "lockout_until"
        private const val KEY_LAST_STEP = "last_accepted_step"
        private const val KEY_STRICT = "strict_mode"

        @Volatile
        private var instance: PactState? = null

        fun get(context: Context): PactState =
            instance ?: synchronized(this) {
                instance ?: PactState(context).also { instance = it }
            }

        /** Millis until the next local midnight — for "rest of today" unlocks. */
        fun untilMidnightMillis(nowMillis: Long = System.currentTimeMillis()): Long {
            val cal = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis - nowMillis
        }
    }
}
