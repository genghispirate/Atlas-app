package com.pact.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import com.pact.app.block.BlockActivity
import com.pact.app.core.PactState

/**
 * The shield. Watches window changes (offline, event-driven, negligible
 * battery) and covers any blocked app with the lock screen the moment it
 * reaches the foreground.
 */
class BlockerService : AccessibilityService() {

    private var lastLaunchPkg: String? = null
    private var lastLaunchAt: Long = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return

        val state = PactState.get(this)
        val now = System.currentTimeMillis()
        if (!state.isBlockedNow(pkg, now)) return

        // Window state events arrive in bursts; don't stack lock screens.
        if (pkg == lastLaunchPkg && now - lastLaunchAt < 1000L) return
        lastLaunchPkg = pkg
        lastLaunchAt = now

        startActivity(
            Intent(this, BlockActivity::class.java)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
                .putExtra(BlockActivity.EXTRA_PACKAGE, pkg)
        )
    }

    override fun onInterrupt() = Unit

    companion object {
        /** Whether the user has enabled the shield in system accessibility settings. */
        fun isEnabled(context: Context): Boolean {
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val me = "${context.packageName}/${BlockerService::class.java.name}"
            return enabled.split(':').any { it.equals(me, ignoreCase = true) }
        }
    }
}
