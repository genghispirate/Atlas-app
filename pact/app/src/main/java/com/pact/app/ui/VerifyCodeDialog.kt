package com.pact.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.unit.dp
import com.pact.app.core.PactState
import com.pact.app.ui.theme.TextSecondary

/**
 * The one gate for anything that loosens the Pact: asks for the guardian's
 * current 6-digit code and reports the verdict, including attempt lockouts.
 */
@Composable
fun VerifyCodeDialog(
    state: PactState,
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    onVerified: () -> Unit,
) {
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var lockedUntil by remember { mutableStateOf(state.snapshot.value.lockoutUntil) }
    val now by rememberNow()
    val isLockedOut = lockedUntil > now

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                CodeInput(
                    value = code,
                    onValueChange = { entered ->
                        code = entered
                        error = null
                        if (entered.length == 6 && !isLockedOut) {
                            when (val result = state.verifyCode(entered)) {
                                is PactState.VerifyResult.Ok -> onVerified()
                                is PactState.VerifyResult.Wrong -> {
                                    code = ""
                                    error = "That code isn't right. Codes refresh every 30 seconds — ask for a fresh one."
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
                    modifier = Modifier.padding(top = 4.dp),
                )
                if (isLockedOut) {
                    Text(
                        "Too many attempts. Try again in ${formatCountdown(lockedUntil - now)}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else if (error != null) {
                    Text(
                        error!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
