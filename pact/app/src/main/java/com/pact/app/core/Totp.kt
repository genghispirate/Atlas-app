package com.pact.app.core

import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * RFC 6238 TOTP, compatible with Google Authenticator and any standard
 * authenticator app (6 digits, 30-second period, HMAC-SHA1).
 *
 * Pure JVM — no Android dependencies — so it is unit-testable on the desktop
 * and trivially auditable. Everything here works fully offline; the only
 * shared state between the two phones is the secret and the clock.
 */
object Totp {

    const val PERIOD_SECONDS = 30L
    const val DIGITS = 6

    /** Accept codes from this many steps before/after now (clock drift + relay time). */
    const val DEFAULT_WINDOW_STEPS = 2

    private const val BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    /** 160-bit random secret, Base32-encoded (32 chars) — the size GA expects for SHA1. */
    fun generateSecret(random: SecureRandom = SecureRandom()): String {
        val bytes = ByteArray(20)
        random.nextBytes(bytes)
        return base32Encode(bytes)
    }

    fun base32Encode(data: ByteArray): String {
        val out = StringBuilder()
        var buffer = 0
        var bitsLeft = 0
        for (b in data) {
            buffer = (buffer shl 8) or (b.toInt() and 0xFF)
            bitsLeft += 8
            while (bitsLeft >= 5) {
                out.append(BASE32_ALPHABET[(buffer shr (bitsLeft - 5)) and 0x1F])
                bitsLeft -= 5
            }
        }
        if (bitsLeft > 0) {
            out.append(BASE32_ALPHABET[(buffer shl (5 - bitsLeft)) and 0x1F])
        }
        return out.toString()
    }

    fun base32Decode(encoded: String): ByteArray {
        val clean = encoded.trim().replace(" ", "").replace("-", "").uppercase().trimEnd('=')
        var buffer = 0
        var bitsLeft = 0
        val out = ArrayList<Byte>(clean.length * 5 / 8)
        for (c in clean) {
            val v = BASE32_ALPHABET.indexOf(c)
            require(v >= 0) { "Invalid Base32 character: $c" }
            buffer = (buffer shl 5) or v
            bitsLeft += 5
            if (bitsLeft >= 8) {
                out.add(((buffer shr (bitsLeft - 8)) and 0xFF).toByte())
                bitsLeft -= 8
            }
        }
        return out.toByteArray()
    }

    fun stepAt(epochMillis: Long): Long = epochMillis / 1000L / PERIOD_SECONDS

    /** The 6-digit code for a given time step. */
    fun codeAt(secretBase32: String, step: Long): String {
        val key = base32Decode(secretBase32)
        val msg = ByteArray(8)
        var v = step
        for (i in 7 downTo 0) {
            msg[i] = (v and 0xFF).toByte()
            v = v ushr 8
        }
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key, "RAW"))
        val hash = mac.doFinal(msg)
        val offset = hash[hash.size - 1].toInt() and 0x0F
        val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
            ((hash[offset + 1].toInt() and 0xFF) shl 16) or
            ((hash[offset + 2].toInt() and 0xFF) shl 8) or
            (hash[offset + 3].toInt() and 0xFF)
        val otp = binary % 1_000_000
        return otp.toString().padStart(DIGITS, '0')
    }

    /**
     * Verify [code] against the current time, allowing [windowSteps] of drift in
     * both directions. Returns the matched step index (for replay protection),
     * or null if the code is wrong. Constant-ish time comparison per candidate.
     */
    fun verify(
        secretBase32: String,
        code: String,
        nowMillis: Long = System.currentTimeMillis(),
        windowSteps: Int = DEFAULT_WINDOW_STEPS,
    ): Long? {
        val entered = code.trim()
        if (entered.length != DIGITS || entered.any { !it.isDigit() }) return null
        val now = stepAt(nowMillis)
        for (delta in -windowSteps..windowSteps) {
            val step = now + delta
            if (step < 0) continue
            if (constantTimeEquals(codeAt(secretBase32, step), entered)) return step
        }
        return null
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].code xor b[i].code)
        return diff == 0
    }

    /** otpauth:// URI that authenticator apps understand when rendered as a QR code. */
    fun otpAuthUri(secretBase32: String, accountName: String, issuer: String = "Pact"): String {
        val account = uriEncode(accountName.ifBlank { "Unlock codes" })
        val iss = uriEncode(issuer)
        return "otpauth://totp/$iss:$account?secret=$secretBase32&issuer=$iss&algorithm=SHA1&digits=$DIGITS&period=$PERIOD_SECONDS"
    }

    private fun uriEncode(s: String): String =
        java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20")

    /** Grouped like "ABCD EFGH ..." for manual entry. */
    fun prettySecret(secretBase32: String): String =
        secretBase32.chunked(4).joinToString(" ")
}
