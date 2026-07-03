package com.pact.app.ui

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pact.app.core.PactState
import com.pact.app.core.Qr
import com.pact.app.core.Totp
import com.pact.app.service.BlockerService
import com.pact.app.ui.theme.Mint
import com.pact.app.ui.theme.Periwinkle
import com.pact.app.ui.theme.Surface2
import com.pact.app.ui.theme.TextSecondary
import com.pact.app.ui.theme.TextTertiary
import kotlinx.coroutines.delay

/**
 * Seven quiet steps: welcome → choose guardian → pair their authenticator →
 * prove it works → grant the shield permission → pick apps → seal the pact.
 * The TOTP secret lives only in memory until setup completes.
 */
@Composable
fun OnboardingFlow(state: PactState, onDone: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    var guardianName by remember { mutableStateOf("") }
    val secret = remember { Totp.generateSecret() }
    var selectedApps by remember { mutableStateOf(setOf<String>()) }

    Column(modifier = Modifier.fillMaxSize()) {
        // progress dots
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 56.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(7) { i ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (i == step) 22.dp else 7.dp, 7.dp)
                        .clip(CircleShape)
                        .background(if (i <= step) Periwinkle else Surface2)
                )
            }
        }

        Box(Modifier.weight(1f)) {
            when (step) {
                0 -> WelcomeStep(onNext = { step = 1 })
                1 -> GuardianStep(
                    name = guardianName,
                    onNameChange = { guardianName = it },
                    onNext = { step = 2 },
                )
                2 -> PairStep(
                    guardianName = guardianName,
                    secret = secret,
                    onNext = { step = 3 },
                )
                3 -> ProveStep(
                    guardianName = guardianName,
                    secret = secret,
                    onNext = { step = 4 },
                    onBack = { step = 2 },
                )
                4 -> PermissionStep(onNext = { step = 5 })
                5 -> PickAppsStep(
                    selected = selectedApps,
                    onSelectedChange = { selectedApps = it },
                    onNext = { step = 6 },
                )
                6 -> SealStep(
                    guardianName = guardianName,
                    appCount = selectedApps.size,
                    onFinish = {
                        state.completeSetup(guardianName, secret, selectedApps)
                        onDone()
                    },
                )
            }
        }
    }
}

@Composable
private fun StepScaffold(
    title: String,
    subtitle: String? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        if (subtitle != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(28.dp))
        content()
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Surface2),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Shield,
                contentDescription = null,
                tint = Periwinkle,
                modifier = Modifier.size(48.dp),
            )
        }
        Spacer(Modifier.height(28.dp))
        Text("Pact", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(10.dp))
        Text(
            "Give the key to someone who cares.",
            style = MaterialTheme.typography.titleMedium,
            color = Periwinkle,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Text(
            "Pact locks the apps that pull you in. The only key is a code from a person you trust — willpower stops being the weak link.\n\nEverything runs on your phone. No account, no internet, no data collected.",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(40.dp))
        PactButton("Make my Pact", onClick = onNext, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun GuardianStep(name: String, onNameChange: (String) -> Unit, onNext: () -> Unit) {
    StepScaffold(
        title = "Choose your keyholder",
        subtitle = "Pick someone who wants you to succeed — a partner, parent, or close friend. They'll hold the codes that unlock your apps, so you'll have to ask them each time.",
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Their name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Periwinkle,
                unfocusedBorderColor = Surface2,
            ),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Choose someone you can reach easily — you'll need them whenever you want back in.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextTertiary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        PactButton(
            "Continue",
            onClick = onNext,
            enabled = name.trim().length >= 2,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PairStep(guardianName: String, secret: String, onNext: () -> Unit) {
    val qr = remember(secret) {
        Qr.encode(Totp.otpAuthUri(secret, guardianName)).asImageBitmap()
    }
    StepScaffold(
        title = "Hand over the key",
        subtitle = "On $guardianName's phone — not yours — open Google Authenticator (or any authenticator app), tap +, choose “Scan a QR code”, and scan this screen.",
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(16.dp),
        ) {
            Image(
                bitmap = qr,
                contentDescription = "Pairing QR code",
                modifier = Modifier.size(240.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Can't scan? $guardianName can type this key in manually:",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            Totp.prettySecret(secret),
            style = MaterialTheme.typography.titleSmall,
            color = Periwinkle,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        PactCard(background = Surface2) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = Mint)
                Spacer(Modifier.width(12.dp))
                Text(
                    "This key appears once, right now. Don't scan it into your own phone — that would give you the key to your own lock.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        PactButton(
            "$guardianName has added it",
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ProveStep(
    guardianName: String,
    secret: String,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    StepScaffold(
        title = "Prove it works",
        subtitle = "Ask $guardianName to read you the 6-digit code showing in their authenticator right now.",
    ) {
        CodeInput(
            value = code,
            onValueChange = { entered ->
                code = entered
                error = false
                if (entered.length == 6) {
                    if (Totp.verify(secret, entered) != null) onNext()
                    else {
                        code = ""
                        error = true
                    }
                }
            },
            isError = error,
        )
        if (error) {
            Spacer(Modifier.height(12.dp))
            Text(
                "That didn't match. Make sure they scanned the QR on the previous step, and try the current code.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(20.dp))
        TextButton(onClick = onBack) { Text("Show the QR again", color = TextSecondary) }
    }
}

@Composable
private fun PermissionStep(onNext: () -> Unit) {
    val context = LocalContext.current
    val serviceOn by produceState(initialValue = BlockerService.isEnabled(context)) {
        while (true) {
            value = BlockerService.isEnabled(context)
            delay(700L)
        }
    }
    StepScaffold(
        title = "Raise the shield",
        subtitle = "Pact needs the accessibility permission to notice when a locked app opens and step in front of it. It never reads your screen and never touches the network.",
    ) {
        PactCard(background = Surface2) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (serviceOn) Icons.Rounded.Check else Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = if (serviceOn) Mint else Periwinkle,
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Pact app shield", style = MaterialTheme.typography.titleSmall)
                    Text(
                        if (serviceOn) "Enabled — you're protected" else "Tap below, find “Pact”, and switch it on",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (serviceOn) Mint else TextSecondary,
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        if (serviceOn) {
            PactButton("Continue", onClick = onNext, modifier = Modifier.fillMaxWidth())
        } else {
            PactButton(
                "Open accessibility settings",
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onNext) { Text("Set up later", color = TextTertiary) }
        }
    }
}

@Composable
private fun PickAppsStep(
    selected: Set<String>,
    onSelectedChange: (Set<String>) -> Unit,
    onNext: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Text(
            "What pulls you in?",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        )
        Box(Modifier.weight(1f)) {
            AppPickerList(selected = selected, onSelectedChange = onSelectedChange)
        }
        PactButton(
            if (selected.isEmpty()) "Choose at least one app"
            else "Lock ${selected.size} ${if (selected.size == 1) "app" else "apps"}",
            onClick = onNext,
            enabled = selected.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        )
    }
}

@Composable
private fun SealStep(guardianName: String, appCount: Int, onFinish: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(Surface2),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = Mint,
                modifier = Modifier.size(48.dp),
            )
        }
        Spacer(Modifier.height(28.dp))
        Text("Your Pact is sealed", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(14.dp))
        Text(
            "$appCount ${if (appCount == 1) "app is" else "apps are"} now locked. From here on, $guardianName holds the key — and that's exactly the point.",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(40.dp))
        PactButton("Begin", onClick = onFinish, modifier = Modifier.fillMaxWidth())
    }
}
