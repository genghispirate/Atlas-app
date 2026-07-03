package com.pact.app.block

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pact.app.core.Apps
import com.pact.app.core.PactState
import com.pact.app.ui.AppIconImage
import com.pact.app.ui.CodeInput
import com.pact.app.ui.PactButton
import com.pact.app.ui.formatCountdown
import com.pact.app.ui.rememberNow
import com.pact.app.ui.theme.Ink
import com.pact.app.ui.theme.Mint
import com.pact.app.ui.theme.PactTheme
import com.pact.app.ui.theme.Periwinkle
import com.pact.app.ui.theme.Surface1
import com.pact.app.ui.theme.Surface2
import com.pact.app.ui.theme.TextSecondary
import com.pact.app.ui.theme.TextTertiary

/**
 * The wall. Shown instantly over any locked app. Back and "leave" both go
 * home; the only way through is a fresh code from the sponsor.
 */
class BlockActivity : ComponentActivity() {

    private val blockedPkg = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        blockedPkg.value = intent.getStringExtra(EXTRA_PACKAGE)
        setContent {
            PactTheme {
                val pkg = blockedPkg.value
                if (pkg == null) {
                    finish()
                } else {
                    BlockScreen(
                        pkg = pkg,
                        onLeave = { goHome() },
                        onUnlocked = { minutes ->
                            val state = PactState.get(this)
                            val duration = if (minutes == UNTIL_TONIGHT) {
                                PactState.untilMidnightMillis()
                            } else {
                                minutes * 60_000L
                            }
                            state.unlockFor(pkg, duration)
                            packageManager.getLaunchIntentForPackage(pkg)?.let { launch ->
                                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(launch)
                            }
                            finish()
                        },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_PACKAGE)?.let { blockedPkg.value = it }
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }

    companion object {
        const val EXTRA_PACKAGE = "pkg"
        const val UNTIL_TONIGHT = -1L
    }
}

private val ENCOURAGEMENTS = listOf(
    "This pause is the whole point.",
    "You asked for this wall when your head was clear.",
    "The urge passes whether or not you feed it.",
    "Five minutes from now you'll be glad you stopped.",
    "You're not missing anything that matters.",
)

@Composable
private fun BlockScreen(
    pkg: String,
    onLeave: () -> Unit,
    onUnlocked: (Long) -> Unit,
) {
    val context = LocalContext.current
    val state = remember { PactState.get(context) }
    val snapshot = state.snapshot.value
    val label = remember(pkg) { Apps.label(context, pkg) }
    val icon = remember(pkg) { Apps.icon(context, pkg) }
    val encouragement = rememberSaveable(pkg) { ENCOURAGEMENTS.random() }

    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var lockedUntil by remember { mutableStateOf(snapshot.lockoutUntil) }
    var verified by remember { mutableStateOf(false) }
    val now by rememberNow()
    val isLockedOut = lockedUntil > now

    BackHandler { onLeave() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Ink, Surface1))),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                AppIconImage(icon, sizeDp = 72)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Periwinkle),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = Ink,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("$label is locked", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text(
                encouragement,
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(36.dp))

            if (!verified) {
                Text(
                    "Ask ${snapshot.guardianName} for the current 6-digit code",
                    style = MaterialTheme.typography.titleSmall,
                    color = Periwinkle,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                CodeInput(
                    value = code,
                    onValueChange = { entered ->
                        code = entered
                        error = null
                        if (entered.length == 6 && !isLockedOut) {
                            when (val result = state.verifyCode(entered)) {
                                is PactState.VerifyResult.Ok -> verified = true
                                is PactState.VerifyResult.Wrong -> {
                                    code = ""
                                    error = "Not quite. Codes change every 30 seconds — ask for a fresh one."
                                }
                                is PactState.VerifyResult.TooManyAttempts -> {
                                    code = ""
                                    lockedUntil = result.untilMillis
                                }
                            }
                        }
                    },
                    enabled = !isLockedOut,
                    isError = error != null,
                )
                Spacer(Modifier.height(14.dp))
                if (isLockedOut) {
                    Text(
                        "Too many attempts. The lock rests for ${formatCountdown(lockedUntil - now)}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                } else if (error != null) {
                    Text(
                        error!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                Text(
                    "Code accepted — how long do you need?",
                    style = MaterialTheme.typography.titleSmall,
                    color = Mint,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(18.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    DurationChoice("5 minutes") { onUnlocked(5) }
                    DurationChoice("15 minutes") { onUnlocked(15) }
                    DurationChoice("1 hour") { onUnlocked(60) }
                    DurationChoice("Until midnight") { onUnlocked(BlockActivity.UNTIL_TONIGHT) }
                }
            }

            Spacer(Modifier.height(36.dp))
            TextButton(onClick = onLeave) {
                Text("Never mind — take me home", color = TextTertiary)
            }
        }
    }
}

@Composable
private fun DurationChoice(label: String, onClick: () -> Unit) {
    PactButton(label, onClick = onClick, tonal = true, modifier = Modifier.fillMaxWidth())
}
