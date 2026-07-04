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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pact.app.R
import com.pact.app.core.Apps
import com.pact.app.core.PactState
import com.pact.app.core.PactState.Tier
import com.pact.app.core.PactState.Trigger
import com.pact.app.ui.theme.Amber
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
import kotlinx.coroutines.delay

/**
 * The wall, rendered inside the service-drawn accessibility overlay.
 *
 * Two difficulties:
 *  - RED: a fresh sponsor code, then a break length.
 *  - YELLOW: a 30-second mindful pause (with optional urge logging), then a
 *    short break — no code, but a cooldown stops back-to-back self-unlocks.
 *    A sponsor code always works as an override.
 */
@Composable
fun BlockWall(
    pkg: String,
    onGoHome: () -> Unit,
    onUnlocked: (durationMillis: Long, tier: Tier, trigger: Trigger?) -> Unit,
) {
    val context = LocalContext.current
    val state = remember { PactState.get(context) }
    val snapshot = state.snapshot.value
    val guardian = snapshot.guardianName
    val tier = snapshot.tierOf(pkg)
    val label = remember(pkg) { Apps.label(context, pkg) }
    val icon = remember(pkg) { Apps.icon(context, pkg) }
    val encouragements = stringArrayResource(R.array.encouragements)
    val encouragement = remember(pkg) { encouragements.random() }
    val now by rememberNow()

    val cooldownUntil = snapshot.yellowCooldownUntil[pkg] ?: 0L
    val yellowResting = tier == Tier.YELLOW && cooldownUntil > now
    var forceCode by remember(pkg) { mutableStateOf(false) }
    val useCodeFlow = tier == Tier.RED || yellowResting && forceCode || tier == Tier.YELLOW && forceCode

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Ink, Surface1))),
    ) {
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
                .padding(horizontal = 28.dp, vertical = 40.dp)
                .wrapContentHeight(),
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
            Text(
                stringResource(R.string.wall_locked_title, label),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                encouragement,
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))

            when {
                useCodeFlow -> CodeFlow(
                    state = state,
                    guardian = guardian,
                    onUnlocked = { duration -> onUnlocked(duration, Tier.RED, null) },
                )
                yellowResting -> {
                    Text(
                        stringResource(
                            R.string.wall_cooldown,
                            formatCountdown(cooldownUntil - now),
                            guardian,
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Amber,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    PactButton(
                        stringResource(R.string.wall_use_code_instead),
                        onClick = { forceCode = true },
                        tonal = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                else -> YellowFlow(
                    onUseCode = { forceCode = true },
                    onUnlocked = { duration, trigger -> onUnlocked(duration, Tier.YELLOW, trigger) },
                )
            }

            Spacer(Modifier.height(24.dp))
            TextButton(onClick = onGoHome) {
                Text(stringResource(R.string.wall_go_home), color = TextTertiary)
            }
        }
    }
}

// ------------------------------------------------------------- yellow flow

@Composable
private fun YellowFlow(
    onUseCode: () -> Unit,
    onUnlocked: (durationMillis: Long, trigger: Trigger?) -> Unit,
) {
    var secondsLeft by remember { mutableIntStateOf(PactState.YELLOW_WAIT_SECONDS) }
    var trigger by remember { mutableStateOf<Trigger?>(null) }
    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000L)
            secondsLeft -= 1
        }
    }
    val waiting = secondsLeft > 0

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        if (waiting) {
            Text(
                stringResource(R.string.wall_pause_title),
                style = MaterialTheme.typography.titleSmall,
                color = Periwinkle,
            )
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(Surface2)
                    .border(2.dp, Periwinkle, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "$secondsLeft",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.wall_pause_body, PactState.YELLOW_WAIT_SECONDS),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
        } else {
            Text(
                stringResource(R.string.wall_pause_done),
                style = MaterialTheme.typography.titleSmall,
                color = Mint,
            )
        }

        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(R.string.wall_whats_pulling),
            style = MaterialTheme.typography.bodyMedium,
            color = TextTertiary,
        )
        Spacer(Modifier.height(10.dp))
        TriggerChips(selected = trigger, onSelect = { trigger = it })

        if (!waiting) {
            Spacer(Modifier.height(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                PactButton(
                    stringResource(R.string.wall_open_5m),
                    onClick = { onUnlocked(5 * 60_000L, trigger) },
                    tonal = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                PactButton(
                    stringResource(R.string.wall_open_15m),
                    onClick = { onUnlocked(15 * 60_000L, trigger) },
                    tonal = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = onUseCode) {
                Text(stringResource(R.string.wall_use_code_instead), color = TextTertiary)
            }
        }
    }
}

@Composable
fun TriggerChips(selected: Trigger?, onSelect: (Trigger?) -> Unit) {
    val labels = mapOf(
        Trigger.BORED to R.string.trigger_bored,
        Trigger.STRESS to R.string.trigger_stress,
        Trigger.HABIT to R.string.trigger_habit,
        Trigger.ANXIETY to R.string.trigger_anxiety,
        Trigger.LONELY to R.string.trigger_lonely,
        Trigger.PROCRASTINATION to R.string.trigger_procrastination,
        Trigger.NEEDED to R.string.trigger_needed,
    )
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        labels.entries.chunked(3).forEach { rowEntries ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowEntries.forEach { (value, labelRes) ->
                    val isSelected = selected == value
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) PactGradient else androidx.compose.ui.graphics.SolidColor(Surface2))
                            .border(
                                1.dp,
                                if (isSelected) Periwinkle else CardBorder,
                                RoundedCornerShape(12.dp),
                            )
                            .clickable { onSelect(if (isSelected) null else value) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text(
                            stringResource(labelRes),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) Ink else TextSecondary,
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- red flow

@Composable
private fun CodeFlow(
    state: PactState,
    guardian: String,
    onUnlocked: (durationMillis: Long) -> Unit,
) {
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var lockedUntil by remember { mutableStateOf(state.snapshot.value.lockoutUntil) }
    var verified by remember { mutableStateOf(false) }
    val now by rememberNow()
    val isLockedOut = lockedUntil > now
    val wrongMessage = stringResource(R.string.verify_wrong)

    fun submit(entered: String) {
        when (val result = state.verifyCode(entered)) {
            is PactState.VerifyResult.Ok -> verified = true
            is PactState.VerifyResult.Wrong -> {
                code = ""
                error = wrongMessage
            }
            is PactState.VerifyResult.TooManyAttempts -> {
                code = ""
                lockedUntil = result.untilMillis
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        if (!verified) {
            Text(
                stringResource(R.string.wall_ask, guardian),
                style = MaterialTheme.typography.titleSmall,
                color = Periwinkle,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            CodeDots(length = code.length, isError = error != null)
            Spacer(Modifier.height(10.dp))
            if (isLockedOut) {
                Text(
                    stringResource(R.string.verify_too_many, formatCountdown(lockedUntil - now)),
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
                stringResource(R.string.wall_accepted),
                style = MaterialTheme.typography.titleSmall,
                color = Mint,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                PactButton(stringResource(R.string.duration_5m), onClick = { onUnlocked(5 * 60_000L) }, tonal = true, modifier = Modifier.fillMaxWidth())
                PactButton(stringResource(R.string.duration_15m), onClick = { onUnlocked(15 * 60_000L) }, tonal = true, modifier = Modifier.fillMaxWidth())
                PactButton(stringResource(R.string.duration_1h), onClick = { onUnlocked(60 * 60_000L) }, tonal = true, modifier = Modifier.fillMaxWidth())
                PactButton(
                    stringResource(R.string.duration_midnight),
                    onClick = { onUnlocked(PactState.untilMidnightMillis()) },
                    tonal = true,
                    modifier = Modifier.fillMaxWidth(),
                )
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
                    contentDescription = stringResource(R.string.cd_delete),
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
