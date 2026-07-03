package com.pact.app.ui

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pact.app.core.PactState
import com.pact.app.core.Qr
import com.pact.app.core.Totp
import com.pact.app.ui.theme.Mint
import com.pact.app.ui.theme.Periwinkle
import com.pact.app.ui.theme.Surface2
import com.pact.app.ui.theme.TextSecondary
import com.pact.app.ui.theme.TextTertiary

private enum class Gate { None, StrictOff, RePair, Reset }

@Composable
fun SettingsScreen(state: PactState, onBack: () -> Unit) {
    val snapshot by state.snapshot.collectAsState()
    var gate by remember { mutableStateOf(Gate.None) }
    var showRePairFlow by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 12.dp, bottom = 16.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = TextSecondary)
            }
            Text("Settings", style = MaterialTheme.typography.headlineSmall)
        }

        SectionLabel("Your keyholder")
        Spacer(Modifier.height(8.dp))
        PactCard {
            Text(snapshot.guardianName, style = MaterialTheme.typography.titleMedium)
            Text("Paired · holds your unlock codes", style = MaterialTheme.typography.bodyMedium, color = Mint)
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = { gate = Gate.RePair }) {
                Text("Change keyholder…", color = Periwinkle)
            }
            Text(
                "Changing your keyholder needs a code from your current one, then pairs a brand-new key.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextTertiary,
            )
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel("Strict mode")
        Spacer(Modifier.height(8.dp))
        PactCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Lock system Settings too", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Stops the shield from being switched off quietly — opening Android Settings will also ask for a code. Turning strict mode ON is free; turning it OFF needs a code.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = snapshot.strictMode,
                    onCheckedChange = { wantOn ->
                        if (wantOn) state.setStrictMode(true) else gate = Gate.StrictOff
                    },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = Periwinkle,
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel("How Pact works")
        Spacer(Modifier.height(8.dp))
        PactCard {
            Text(
                "Pact runs entirely on this phone — no account, no server, no internet needed. " +
                    "Unlock codes are standard authenticator codes (TOTP): your keyholder's phone " +
                    "and yours each compute the same 6-digit code from a shared key and the clock, " +
                    "so codes work even when both phones are offline.\n\n" +
                    "The key was shown exactly once, during setup, and only your keyholder has it. " +
                    "It is stored on this phone encrypted inside the Android Keystore.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel("Danger zone")
        Spacer(Modifier.height(8.dp))
        PactCard {
            TextButton(onClick = { gate = Gate.Reset }) {
                Text("End the Pact & reset everything…", color = MaterialTheme.colorScheme.error)
            }
            Text(
                "Removes all locks and returns to setup. Needs a code from ${snapshot.guardianName}.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextTertiary,
            )
        }
        Spacer(Modifier.height(32.dp))
    }

    when (gate) {
        Gate.None -> Unit
        Gate.StrictOff -> VerifyCodeDialog(
            state = state,
            title = "Turn off strict mode?",
            subtitle = "Ask ${snapshot.guardianName} for the current code.",
            onDismiss = { gate = Gate.None },
            onVerified = {
                state.setStrictMode(false)
                gate = Gate.None
            },
        )
        Gate.RePair -> VerifyCodeDialog(
            state = state,
            title = "Change keyholder?",
            subtitle = "First, a code from ${snapshot.guardianName} to unlock the change.",
            onDismiss = { gate = Gate.None },
            onVerified = {
                gate = Gate.None
                showRePairFlow = true
            },
        )
        Gate.Reset -> VerifyCodeDialog(
            state = state,
            title = "End the Pact?",
            subtitle = "Ask ${snapshot.guardianName} for the current code to remove all locks and reset.",
            onDismiss = { gate = Gate.None },
            onVerified = {
                state.reset()
                gate = Gate.None
            },
        )
    }

    if (showRePairFlow) {
        RePairDialog(state = state, onDismiss = { showRePairFlow = false })
    }
}

/** Pair a new keyholder: name → QR → verify a code from the new device. */
@Composable
private fun RePairDialog(state: PactState, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var stage by remember { mutableStateOf(0) }
    val newSecret = remember { Totp.generateSecret() }
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                when (stage) {
                    0 -> "New keyholder"
                    1 -> "Scan on their phone"
                    else -> "Prove it works"
                },
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                when (stage) {
                    0 -> {
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
                    1 -> {
                        val qr = remember(newSecret, name) {
                            Qr.encode(Totp.otpAuthUri(newSecret, name)).asImageBitmap()
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.White)
                                .padding(12.dp),
                        ) {
                            Image(bitmap = qr, contentDescription = "Pairing QR code", modifier = Modifier.size(200.dp))
                        }
                        Text(
                            "On $name's phone, add this to Google Authenticator (tap +, scan QR). Manual key:\n${Totp.prettySecret(newSecret)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                        )
                    }
                    else -> {
                        Text(
                            "Enter the current code from $name's authenticator to seal the new Pact.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                        )
                        CodeInput(
                            value = code,
                            onValueChange = { entered ->
                                code = entered
                                error = false
                                if (entered.length == 6) {
                                    if (Totp.verify(newSecret, entered) != null) {
                                        state.rePair(name, newSecret)
                                        onDismiss()
                                    } else {
                                        code = ""
                                        error = true
                                    }
                                }
                            },
                            isError = error,
                        )
                        if (error) {
                            Text(
                                "That didn't match — try the current code.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (stage < 2) {
                TextButton(
                    onClick = { stage += 1 },
                    enabled = stage != 0 || name.trim().length >= 2,
                ) { Text(if (stage == 0) "Next" else "They've added it") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
