package com.pact.app.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import com.pact.app.core.PactState

/**
 * The shield. Watches window changes (offline, event-driven, negligible
 * battery) and covers any blocked app with the lock wall the moment it
 * reaches the foreground. The wall is an accessibility overlay drawn by this
 * service — see [BlockOverlay] for why that matters.
 */
class BlockerService : AccessibilityService() {

    private val overlay by lazy { BlockOverlay(this) }
    private var lastBlockPkg: String? = null
    private var lastBlockAt: Long = 0L

    private val homePackage: String? by lazy {
        packageManager.resolveActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            PackageManager.MATCH_DEFAULT_ONLY,
        )?.activityInfo?.packageName
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName || pkg == SYSTEM_UI_PACKAGE) return

        val state = PactState.get(this)
        val now = System.currentTimeMillis()

        if (state.isBlockedNow(pkg, now)) {
            // Events arrive in bursts; count one intervention per second per app.
            if (pkg != lastBlockPkg || now - lastBlockAt >= 1000L) {
                state.recordBlock(pkg)
            }
            lastBlockPkg = pkg
            lastBlockAt = now
            val shown = overlay.show(pkg)
            if (!shown) {
                // Overlay failed (shouldn't happen) — at least bounce to home.
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
            return
        }

        // A non-blocked window came forward. Only dismiss the wall when a real
        // app or the launcher took over — keyboards, dialogs, and system
        // windows must not tear the wall down.
        if (overlay.isShowing) {
            val isRealApp = pkg == homePackage ||
                packageManager.getLaunchIntentForPackage(pkg) != null
            if (isRealApp) overlay.dismiss()
        }
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        overlay.dismiss()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        overlay.dismiss()
        super.onDestroy()
    }

    companion object {
        private const val SYSTEM_UI_PACKAGE = "com.android.systemui"

        /**
         * Whether the shield is enabled in system accessibility settings.
         * Devices report entries in either full or short component form, so
         * compare parsed ComponentNames rather than raw strings.
         */
        fun isEnabled(context: Context): Boolean {
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val me = ComponentName(context, BlockerService::class.java)
            return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
        }
    }
}
