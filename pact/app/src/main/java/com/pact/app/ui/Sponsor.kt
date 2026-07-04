package com.pact.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pact.app.core.PactState
import com.pact.app.core.Totp
import com.pact.app.ui.theme.Amber
import com.pact.app.ui.theme.Mint
import com.pact.app.ui.theme.Periwinkle
import com.pact.app.ui.theme.Surface2
import com.pact.app.ui.theme.Surface3
import com.pact.app.ui.theme.TextSecondary
import com.pact.app.ui.theme.TextTertiary

/**
 * Sponsor mode: this phone belongs to the trusted person. It holds keys for
 * one or more people and shows the live 6-digit unlock codes — no separate
 * authenticator app needed. Works fully offline, exactly like a 2FA app.
 */

// -------------------------------------------------------------- setup flow

@Composable
fun SponsorSetupFlow(state: PactState, onBack: () -> Unit, onDone: () -> Unit) {
    var captured by remember { mutableStateOf<String?>(null) }
    var scanning by remember { mutableStateOf(false) }

    if (scanning) {
        ScanScreen(
            title = "Scan their pairing QR",
            onResult = { content ->
                scanning = false
                Totp.extractSecret(content)?.let { captured = it }
            },
            onClose = { scanning = false },
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = TextSecondary)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("You hold the key", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text(
            "Someone you care about is locking their distracting apps, and they've chosen you as their sponsor. When they want back in, they'll ask you for the 6-digit code shown here.\n\nTheir phone shows a QR code during setup — scan it below.",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
        )
        Spacer(Modifier.height(28.dp))
        AddKeyContent(
            onScan = { scanning = true },
            onCaptured = { secret -> captured = secret },
        )
        Spacer(Modifier.height(24.dp))
    }

    captured?.let { secret ->
        NameSponseeDialog(
            onDismiss = { captured = null },
            onNamed = { name ->
                state.becomeSponsor()
                state.addSponsee(name, secret)
                onDone()
            },
        )
    }
}

/** Scan button + manual key fallback. Calls [onCaptured] with a valid secret. */
@Composable
fun AddKeyContent(onScan: () -> Unit, onCaptured: (String) -> Unit) {
    var manualKey by remember { mutableStateOf("") }
    var manualError by remember { mutableStateOf(false) }
    var showManual by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        PactCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, tint = Periwinkle)
                Spacer(Modifier.width(12.dp))
                Text(
                    "Point your camera at the QR code on their phone. It's shown during their setup, at the “Hand over the key” step.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
            Spacer(Modifier.height(16.dp))
            PactButton("Scan their QR code", onClick = onScan, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(12.dp))
        if (!showManual) {
            TextButton(onClick = { showManual = true }) {
                Text("Camera not working? Type the key instead", color = TextTertiary)
            }
        } else {
            PactCard {
                Text(
                    "Their pairing screen also shows the key as letters — type or paste it here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = manualKey,
                    onValueChange = {
                        manualKey = it
                        manualError = false
                    },
                    label = { Text("Key (e.g. ABCD EFGH …)") },
                    singleLine = true,
                    isError = manualError,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Periwinkle,
                        unfocusedBorderColor = Surface2,
                    ),
                )
                if (manualError) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "That doesn't look like a valid key — it's 32 letters and digits.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Spacer(Modifier.height(12.dp))
                PactButton(
                    "Add key",
                    onClick = {
                        val secret = Totp.extractSecret(manualKey)
                        if (secret != null) onCaptured(secret) else manualError = true
                    },
                    tonal = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun NameSponseeDialog(onDismiss: () -> Unit, onNamed: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Key added ✓", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column {
                Text(
                    "Whose apps does this key unlock?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Their name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Periwinkle,
                        unfocusedBorderColor = Surface2,
                    ),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onNamed(name) },
                enabled = name.trim().length >= 2,
            ) { Text("Done") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

// -------------------------------------------------------------------- home

@Composable
fun SponsorHome(state: PactState) {
    val snapshot by state.snapshot.collectAsState()
    var adding by remember { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(false) }
    var newSecret by remember { mutableStateOf<String?>(null) }
    var removing by remember { mutableStateOf<PactState.Sponsee?>(null) }

    if (scanning) {
        ScanScreen(
            title = "Scan their pairing QR",
            onResult = { content ->
                scanning = false
                adding = false
                Totp.extractSecret(content)?.let { newSecret = it }
            },
            onClose = { scanning = false },
        )
        return
    }

    if (adding) {
        BackHandler { adding = false }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                IconButton(onClick = { adding = false }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = TextSecondary)
                }
                Text("Add a person", style = MaterialTheme.typography.headlineSmall)
            }
            Spacer(Modifier.height(16.dp))
            AddKeyContent(
                onScan = { scanning = true },
                onCaptured = { secret ->
                    adding = false
                    newSecret = secret
                },
            )
            Spacer(Modifier.height(24.dp))
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp),
            ) {
                PactLogo(36)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Pact", style = MaterialTheme.typography.headlineSmall)
                    Text("Sponsor", style = MaterialTheme.typography.labelMedium, color = Periwinkle)
                }
                IconButton(onClick = { adding = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add a person", tint = TextSecondary)
                }
            }
            Text(
                "When they ask to unlock, read them the code. It changes every 30 seconds and works without internet.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextTertiary,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(snapshot.sponsees, key = { it.name + it.secretBlob.hashCode() }) { sponsee ->
                    SponseeCodeCard(
                        state = state,
                        sponsee = sponsee,
                        onRemove = { removing = sponsee },
                    )
                }
                if (snapshot.sponsees.isEmpty()) {
                    item {
                        PactCard {
                            Text(
                                "No keys yet. Tap + to scan a pairing QR from someone's phone.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }

    newSecret?.let { secret ->
        NameSponseeDialog(
            onDismiss = { newSecret = null },
            onNamed = { name ->
                state.addSponsee(name, secret)
                newSecret = null
            },
        )
    }

    removing?.let { sponsee ->
        AlertDialog(
            onDismissRequest = { removing = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Remove ${sponsee.name}?", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Text(
                    "You'll no longer be able to give ${sponsee.name} unlock codes. They would need to re-pair with a sponsor to change anything.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    state.removeSponsee(sponsee.name)
                    removing = null
                }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { removing = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SponseeCodeCard(
    state: PactState,
    sponsee: PactState.Sponsee,
    onRemove: () -> Unit,
) {
    val secret = remember(sponsee.secretBlob) { state.sponseeSecret(sponsee) }
    val now by rememberNowFast()

    PactCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Key, contentDescription = null, tint = Periwinkle, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(sponsee.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Rounded.Close, contentDescription = "Remove", tint = TextTertiary, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(14.dp))
        if (secret == null) {
            Text(
                "Couldn't read this key from secure storage.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        } else {
            val code = Totp.codeAt(secret, Totp.stepAt(now))
            val remainingMillis = 30_000L - (now % 30_000L)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${code.take(3)} ${code.drop(3)}",
                    style = MaterialTheme.typography.displaySmall,
                    letterSpacing = 3.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                CountdownRing(
                    fraction = remainingMillis / 30_000f,
                    secondsLeft = ((remainingMillis + 999) / 1000).toInt(),
                )
            }
        }
    }
}

@Composable
private fun CountdownRing(fraction: Float, secondsLeft: Int) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(52.dp)) {
        Canvas(Modifier.size(52.dp)) {
            val stroke = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
            val inset = 5.dp.toPx()
            val arcSize = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2)
            drawArc(
                color = Surface3,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = stroke,
            )
            drawArc(
                color = if (fraction < 0.2f) Amber else Mint,
                startAngle = -90f,
                sweepAngle = 360f * fraction,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = stroke,
            )
        }
        Text(
            "$secondsLeft",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
        )
    }
}
