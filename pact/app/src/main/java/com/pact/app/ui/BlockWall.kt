package com.pact.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pact.app.core.Apps
import com.pact.app.core.PactState
import com.pact.app.ui.theme.CardBorder
import com.pact.app.ui.theme.Ink
import com.pact.app.ui.theme.Mint
import com.pact.app.ui.theme.PactGradient
import com.pact.app.ui.theme.Periwinkle
import com.pact.app.ui.theme.Surface1
import com.pact.app.ui.theme.Surface2
import com.pact.app.ui.theme.TextSecondary
import com.pact.app.ui.theme.TextTertiary
import com.pact.app.ui.theme.Violet

/**
 * The wall. Rendered inside a TYPE_ACCESSIBILITY_OVERLAY window drawn by the
 * shield itself, so it appears instantly over any locked app and cannot be
 * suppressed by background-activity-launch restrictions. Code entry uses a
 * built-in PIN pad — no reliance on the system keyboard inside an overlay.
 */

private val ENCOURAGEMENTS = listOf(
    "This pause is the whole point.",
    "You asked for this wall when your head was clear.",
    "The urge passes whether or not you feed it.",
    "Five minutes from now you'll be glad you stopped.",
    "You're not missing anything that matters.",
)

@Composable
fun BlockWall(
    pkg: String,
    onGoHome: () -> Unit,
    onUnlocked: (durationMillis: Long) -> Unit,
) {
    val context = LocalContext.current
    val state = remember { PactState.get(context) }
    val guardian = state.snapshot.value.guardianName
    val label = remember(pkg) { Apps.label(context, pkg) }
    val icon = remember(pkg) { Apps.icon(context, pkg) }
    val encouragement = remember(pkg) { ENCOURAGEMENTS.random() }

    var code by remember(pkg) { mutableStateOf("") }
    var error by remember(pkg) { mutableStateOf<String?>(null) }
    var lockedUntil by remember { mutableStateOf(state.snapshot.value.lockoutUntil) }
    var verified by remember(pkg) { mutableStateOf(false) }
    val now by rememberNow()
    val isLockedOut = lockedUntil > now

    fun submit(entered: String) {
        when (val result = state.verifyCode(entered)) {
            is PactState.VerifyResult.Ok -> verified = true
            is PactState.VerifyResult.Wrong -> {
                code = ""
                error = "Not quite — codes change every 30 seconds. Ask for a fresh one."
            }
            is PactState.VerifyResult.TooManyAttempts -> {
                code = ""
                lockedUntil = result.untilMillis
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Ink, Surface1))),
    ) {
        // ambient glow behind the header
        Box(
            modifier = Modifier
                .size(340.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.radialGradient(
                        listOf(Violet.copy(alpha = 0.22f), Violet.copy(alpha = 0f))
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                AppIconImage(icon, sizeDp = 72)
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(PactGradient),
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
            Spacer(Modifier.height(20.dp))
            Text("$label is locked", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                encouragement,
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))

            if (!verified) {
                Text(
                    "Ask $guardian for the current code",
                    style = MaterialTheme.typography.titleSmall,
                    color = Periwinkle,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(18.dp))
                CodeDots(length = code.length, isError = error != null)
                Spacer(Modifier.height(10.dp))
                if (isLockedOut) {
                    Text(
                        "Too many attempts. The lock rests for ${formatCountdown(lockedUntil - now)}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Text(
                        error ?: " ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(14.dp))
                PinPad(
                    enabled = !isLockedOut,
                    onDigit = { digit ->
                        if (code.length < 6) {
                            error = null
                            code += digit
                            if (code.length == 6) submit(code)
                        }
                    },
                    onBackspace = {
                        error = null
                        code = code.dropLast(1)
                    },
                )
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
                    PactButton("5 minutes", onClick = { onUnlocked(5 * 60_000L) }, tonal = true, modifier = Modifier.fillMaxWidth())
                    PactButton("15 minutes", onClick = { onUnlocked(15 * 60_000L) }, tonal = true, modifier = Modifier.fillMaxWidth())
                    PactButton("1 hour", onClick = { onUnlocked(60 * 60_000L) }, tonal = true, modifier = Modifier.fillMaxWidth())
                    PactButton(
                        "Until midnight",
                        onClick = { onUnlocked(PactState.untilMidnightMillis()) },
                        tonal = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            TextButton(onClick = onGoHome) {
                Text("Never mind — take me home", color = TextTertiary)
            }
        }
    }
}

/** Six dots that fill as digits are entered — PIN-screen style. */
@Composable
fun CodeDots(length: Int, isError: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(6) { i ->
            val filled = i < length
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .then(
                        when {
                            isError -> Modifier.background(MaterialTheme.colorScheme.error)
                            filled -> Modifier.background(PactGradient)
                            else -> Modifier
                                .background(Surface2)
                                .border(1.dp, CardBorder, CircleShape)
                        }
                    )
            )
        }
    }
}

/** On-screen numeric pad — works everywhere, including service overlays. */
@Composable
fun PinPad(
    enabled: Boolean,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        listOf("123", "456", "789").forEach { rowDigits ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                rowDigits.forEach { d ->
                    PadKey(enabled = enabled, onClick = { onDigit(d) }) {
                        Text(
                            d.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Spacer(Modifier.size(72.dp))
            PadKey(enabled = enabled, onClick = { onDigit('0') }) {
                Text(
                    "0",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            PadKey(enabled = enabled, onClick = onBackspace) {
                Icon(
                    Icons.AutoMirrored.Rounded.Backspace,
                    contentDescription = "Delete",
                    tint = TextSecondary,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}

@Composable
private fun PadKey(
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(Surface2)
            .border(1.dp, CardBorder, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
