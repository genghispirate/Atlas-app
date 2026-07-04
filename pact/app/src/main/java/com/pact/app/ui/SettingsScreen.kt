package com.pact.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pact.app.R
import com.pact.app.core.Backup
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
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 12.dp, bottom = 16.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.common_back),
                    tint = TextSecondary,
                )
            }
            Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall)
        }

        SectionLabel(stringResource(R.string.settings_sponsor_section))
        Spacer(Modifier.height(8.dp))
        PactCard {
            Text(snapshot.guardianName, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.settings_paired),
                style = MaterialTheme.typography.bodyMedium,
                color = Mint,
            )
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = { gate = Gate.RePair }) {
                Text(stringResource(R.string.settings_change_sponsor), color = Periwinkle)
            }
            Text(
                stringResource(R.string.settings_change_sponsor_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = TextTertiary,
            )
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel(stringResource(R.string.settings_strict_section))
        Spacer(Modifier.height(8.dp))
        PactCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.strict_title), style = MaterialTheme.typography.titleSmall)
                    Text(
                        stringResource(R.string.strict_body),
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
        SectionLabel(stringResource(R.string.settings_how_section))
        Spacer(Modifier.height(8.dp))
        PactCard {
            Text(
                stringResource(R.string.how_body),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel(stringResource(R.string.backup_section))
        Spacer(Modifier.height(8.dp))
        BackupCard(state = state)

        Spacer(Modifier.height(20.dp))
        SectionLabel(stringResource(R.string.settings_danger))
        Spacer(Modifier.height(8.dp))
        PactCard {
            TextButton(onClick = { gate = Gate.Reset }) {
                Text(stringResource(R.string.reset_action), color = MaterialTheme.colorScheme.error)
            }
            Text(
                stringResource(R.string.reset_hint, snapshot.guardianName),
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
            title = stringResource(R.string.strict_off_title),
            subtitle = stringResource(R.string.ask_code, snapshot.guardianName),
            onDismiss = { gate = Gate.None },
            onVerified = {
                state.setStrictMode(false)
                gate = Gate.None
            },
        )
        Gate.RePair -> VerifyCodeDialog(
            state = state,
            title = stringResource(R.string.change_sponsor_title),
            subtitle = stringResource(R.string.change_sponsor_body, snapshot.guardianName),
            onDismiss = { gate = Gate.None },
            onVerified = {
                gate = Gate.None
                showRePairFlow = true
            },
        )
        Gate.Reset -> VerifyCodeDialog(
            state = state,
            title = stringResource(R.string.end_pact_title),
            subtitle = stringResource(R.string.end_pact_body, snapshot.guardianName),
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

/** Encrypted export/import plus a stats-only CSV. All through the system file picker. */
@Composable
private fun BackupCard(state: PactState) {
    val context = LocalContext.current
    var passphraseFor by remember { mutableStateOf<BackupAction?>(null) }
    var passphrase by remember { mutableStateOf("") }
    var pendingImport by remember { mutableStateOf<android.net.Uri?>(null) }

    fun toast(resId: Int) =
        android.widget.Toast.makeText(context, context.getString(resId), android.widget.Toast.LENGTH_LONG).show()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null && passphrase.isNotEmpty()) {
            runCatching {
                val json = Backup.export(state.snapshot.value).toString()
                val blob = Backup.encrypt(json, passphrase.toCharArray())
                context.contentResolver.openOutputStream(uri)?.use { it.write(blob.toByteArray()) }
                toast(R.string.backup_done)
            }.onFailure { toast(R.string.backup_failed) }
        }
        passphrase = ""
    }

    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)
                    ?.use { it.write(Backup.statsCsv(state.snapshot.value).toByteArray()) }
                toast(R.string.backup_done)
            }.onFailure { toast(R.string.backup_failed) }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingImport = uri
            passphraseFor = BackupAction.IMPORT
        }
    }

    PactCard {
        TextButton(onClick = { passphraseFor = BackupAction.EXPORT }) {
            Text(stringResource(R.string.backup_export), color = Periwinkle)
        }
        Text(
            stringResource(R.string.backup_export_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = TextTertiary,
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { csvLauncher.launch("pact-stats.csv") }) {
            Text(stringResource(R.string.backup_export_csv), color = Periwinkle)
        }
        TextButton(onClick = { importLauncher.launch(arrayOf("*/*")) }) {
            Text(stringResource(R.string.backup_import), color = Periwinkle)
        }
        Text(
            stringResource(R.string.backup_import_note),
            style = MaterialTheme.typography.bodyMedium,
            color = TextTertiary,
        )
    }

    passphraseFor?.let { action ->
        AlertDialog(
            onDismissRequest = {
                passphraseFor = null
                passphrase = ""
            },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(R.string.backup_passphrase), style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column {
                    Text(
                        stringResource(R.string.backup_passphrase_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = { passphrase = it },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
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
                    enabled = passphrase.length >= 4,
                    onClick = {
                        when (action) {
                            BackupAction.EXPORT -> {
                                passphraseFor = null
                                exportLauncher.launch("pact-backup.pact")
                            }
                            BackupAction.IMPORT -> {
                                val uri = pendingImport
                                passphraseFor = null
                                if (uri != null) {
                                    runCatching {
                                        val blob = context.contentResolver.openInputStream(uri)
                                            ?.use { it.readBytes().toString(Charsets.UTF_8) }
                                            ?: error("empty")
                                        val json = Backup.decrypt(blob, passphrase.toCharArray())
                                            ?: error("bad passphrase")
                                        if (state.applyBackup(org.json.JSONObject(json))) {
                                            toast(R.string.backup_restored)
                                        } else {
                                            toast(R.string.backup_failed)
                                        }
                                    }.onFailure { toast(R.string.backup_failed) }
                                    passphrase = ""
                                    pendingImport = null
                                }
                            }
                        }
                    },
                ) {
                    Text(
                        stringResource(
                            if (action == BackupAction.EXPORT) R.string.backup_encrypt_and_save
                            else R.string.backup_decrypt_and_restore
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    passphraseFor = null
                    passphrase = ""
                }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}

private enum class BackupAction { EXPORT, IMPORT }

/** Pair a new sponsor: name → QR → verify a code from the new device. */
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
                stringResource(
                    when (stage) {
                        0 -> R.string.repair_new_sponsor
                        1 -> R.string.repair_scan_title
                        else -> R.string.prove_title
                    }
                ),
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
                            label = { Text(stringResource(R.string.guardian_name_label)) },
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
                            Image(
                                bitmap = qr,
                                contentDescription = stringResource(R.string.pair_qr_desc),
                                modifier = Modifier.size(200.dp),
                            )
                        }
                        Text(
                            stringResource(R.string.repair_body_qr, name, Totp.prettySecret(newSecret)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                        )
                    }
                    else -> {
                        Text(
                            stringResource(R.string.repair_enter_code, name),
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
                            autoFocus = true,
                        )
                        if (error) {
                            Text(
                                stringResource(R.string.repair_wrong),
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
                ) {
                    Text(
                        stringResource(
                            if (stage == 0) R.string.common_next else R.string.they_added_it
                        )
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}
