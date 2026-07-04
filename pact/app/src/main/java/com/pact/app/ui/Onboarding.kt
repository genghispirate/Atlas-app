package com.pact.app.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.pact.app.core.PactState
import com.pact.app.core.Qr
import com.pact.app.core.Totp
import com.pact.app.service.BlockerService
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * v1.1 flow: a real introduction first (3 swipeable pages), then a role
 * choice — block my own apps, or hold the keys as a sponsor — then the
 * matching guided setup. The TOTP secret lives only in memory until setup
 * completes.
 */

private enum class Phase { Intro, RoleSelect, UserSetup, SponsorSetup }

@Composable
fun OnboardingFlow(state: PactState, onDone: () -> Unit) {
    var phase by remember { mutableStateOf(Phase.Intro) }
    when (phase) {
        Phase.Intro -> IntroPager(onFinished = { phase = Phase.RoleSelect })
        Phase.RoleSelect -> RoleSelect(
            onUser = { phase = Phase.UserSetup },
            onSponsor = { phase = Phase.SponsorSetup },
        )
        Phase.UserSetup -> UserSetupFlow(state = state, onDone = onDone)
        Phase.SponsorSetup -> SponsorSetupFlow(
            state = state,
            onBack = { phase = Phase.RoleSelect },
            onDone = onDone,
        )
    }
}

// ------------------------------------------------------------------- brand

/** The in-app logo mark: gradient rounded square with a shield. */
@Composable
fun PactLogo(sizeDp: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(RoundedCornerShape((sizeDp * 30 / 100).dp))
            .background(PactGradient),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.Shield,
            contentDescription = null,
            tint = Color(0xFFF4F6FF),
            modifier = Modifier.size((sizeDp * 52 / 100).dp),
        )
        Icon(
            Icons.Rounded.Key,
            contentDescription = null,
            tint = com.pact.app.ui.theme.VioletDeep,
            modifier = Modifier.size((sizeDp * 22 / 100).dp),
        )
    }
}

// -------------------------------------------------------------------- intro

private data class IntroPage(
    val art: @Composable () -> Unit,
    val title: String,
    val body: String,
)

@Composable
private fun IntroPager(onFinished: () -> Unit) {
    val pages = listOf(
        IntroPage(
            { ArtLockedPhone() },
            "Lock what pulls you in",
            "Pact blocks the apps you can't put down — Instagram, TikTok, YouTube, anything you choose. They stay locked until someone lets you back in.",
        ),
        IntroPage(
            { ArtTwoPeople() },
            "A person, not a password",
            "You pick a sponsor — a partner, parent, or close friend. Only a fresh 6-digit code from them can unlock your apps. Willpower stops being the weak link.",
        ),
        IntroPage(
            { ArtOfflineShield() },
            "Private, offline, yours",
            "No account. No internet needed — ever. Codes work like 2FA codes, so your sponsor can read one out over any phone call. Nothing leaves your phone.",
        ),
    )
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 64.dp, start = 28.dp, end = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PactLogo(40)
            Spacer(Modifier.width(12.dp))
            Text("Pact", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.weight(1f))
            if (pagerState.currentPage < pages.size - 1) {
                TextButton(onClick = onFinished) { Text("Skip", color = TextTertiary) }
            }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            val p = pages[page]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                p.art()
                Spacer(Modifier.height(28.dp))
                Text(p.title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Spacer(Modifier.height(14.dp))
                Text(
                    p.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }

        // dots + CTA
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(pages.size) { i ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (i == pagerState.currentPage) 24.dp else 8.dp, 8.dp)
                        .clip(CircleShape)
                        .background(if (i == pagerState.currentPage) Periwinkle else Surface2)
                )
            }
        }
        PactButton(
            text = if (pagerState.currentPage == pages.size - 1) "Get started" else "Next",
            onClick = {
                if (pagerState.currentPage == pages.size - 1) onFinished()
                else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 20.dp),
        )
    }
}

// -------------------------------------------------------------- role select

@Composable
private fun RoleSelect(onUser: () -> Unit, onSponsor: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Who is this phone for?", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(
            "Pact has two sides — the person locking their apps, and the person holding the key.",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(36.dp))

        RoleCard(
            icon = Icons.Rounded.Lock,
            iconOnGradient = true,
            title = "Block my apps",
            body = "I want to lock distracting apps and give the key to someone I trust.",
            onClick = onUser,
        )
        Spacer(Modifier.height(14.dp))
        RoleCard(
            icon = Icons.Rounded.Key,
            iconOnGradient = false,
            title = "I'm the sponsor",
            body = "Someone I care about asked me to hold their unlock codes.",
            onClick = onSponsor,
        )
    }
}

@Composable
private fun RoleCard(
    icon: ImageVector,
    iconOnGradient: Boolean,
    title: String,
    body: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Surface1)
            .border(1.dp, CardBorder, shape)
            .clickable(onClick = onClick)
            .padding(20.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (iconOnGradient) PactGradient else androidx.compose.ui.graphics.SolidColor(Surface2)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (iconOnGradient) Ink else Periwinkle,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    }
}

// ----------------------------------------------------------- user setup flow

@Composable
private fun UserSetupFlow(state: PactState, onDone: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    var guardianName by remember { mutableStateOf("") }
    val secret = remember { Totp.generateSecret() }
    var selectedApps by remember { mutableStateOf(setOf<String>()) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 56.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(6) { i ->
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
                0 -> GuardianStep(
                    name = guardianName,
                    onNameChange = { guardianName = it },
                    onNext = { step = 1 },
                )
                1 -> PairStep(
                    guardianName = guardianName,
                    secret = secret,
                    onNext = { step = 2 },
                )
                2 -> ProveStep(
                    guardianName = guardianName,
                    secret = secret,
                    onNext = { step = 3 },
                    onBack = { step = 1 },
                )
                3 -> PermissionStep(onNext = { step = 4 })
                4 -> PickAppsStep(
                    selected = selectedApps,
                    onSelectedChange = { selectedApps = it },
                    onNext = { step = 5 },
                )
                5 -> SealStep(
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
private fun GuardianStep(name: String, onNameChange: (String) -> Unit, onNext: () -> Unit) {
    StepScaffold(
        title = "Choose your sponsor",
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
        subtitle = "This QR code is the key to your locks. It appears once — right now — and only $guardianName should have it.",
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
                modifier = Modifier.size(220.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        PactCard {
            Text("On $guardianName's phone:", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(12.dp))
            NumberedStep(1, buildAnnotatedString {
                append("Install ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Periwinkle)) { append("Pact") }
                append(" (this app) on their phone")
            })
            Spacer(Modifier.height(10.dp))
            NumberedStep(2, buildAnnotatedString {
                append("During setup, choose ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Periwinkle)) { append("“I'm the sponsor”") }
            })
            Spacer(Modifier.height(10.dp))
            NumberedStep(3, buildAnnotatedString {
                append("Scan this QR with the camera button there")
            })
            Spacer(Modifier.height(14.dp))
            Text(
                "Prefer not to install anything? Google Authenticator or any 2FA app can scan this same QR. Manual key: ${Totp.prettySecret(secret)}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextTertiary,
            )
        }
        Spacer(Modifier.height(16.dp))
        PactCard(background = Surface2) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = Mint)
                Spacer(Modifier.width(12.dp))
                Text(
                    "Don't scan it into your own phone — that would give you the key to your own lock.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        PactButton(
            "$guardianName has the key",
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
        subtitle = "Ask $guardianName for the 6-digit code showing on their phone right now.",
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
        subtitle = "One Android permission lets Pact notice when a locked app opens so it can step in front. Here's exactly how to turn it on:",
    ) {
        PactCard {
            NumberedStep(1, buildAnnotatedString {
                append("Tap ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Periwinkle)) { append("“Open settings”") }
                append(" below")
            })
            Spacer(Modifier.height(12.dp))
            NumberedStep(2, buildAnnotatedString {
                append("Find ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Periwinkle)) { append("Pact app shield") }
                append(" in the list — it may sit under “Installed apps” or “Downloaded apps”")
            })
            Spacer(Modifier.height(12.dp))
            NumberedStep(3, buildAnnotatedString {
                append("Tap it and switch it ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Periwinkle)) { append("On") }
            })
            Spacer(Modifier.height(12.dp))
            NumberedStep(4, buildAnnotatedString {
                append("Android will show a warning — ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("that's normal") }
                append(". Pact only detects which app opens; it never reads what's on your screen. Tap ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Periwinkle)) { append("Allow") }
            })
            Spacer(Modifier.height(12.dp))
            NumberedStep(5, buildAnnotatedString {
                append("Come back here — this page will turn green ✓")
            })
        }
        Spacer(Modifier.height(16.dp))
        PactCard(background = if (serviceOn) Surface2 else Surface1) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (serviceOn) Icons.Rounded.Check else Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = if (serviceOn) Mint else Amber,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    if (serviceOn) "The shield is up — you're protected." else "Waiting for the shield…",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (serviceOn) Mint else TextSecondary,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        if (serviceOn) {
            PactButton("Continue", onClick = onNext, modifier = Modifier.fillMaxWidth())
        } else {
            PactButton(
                "Open settings",
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
                .size(104.dp)
                .clip(CircleShape)
                .background(PactGradient),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = Ink,
                modifier = Modifier.size(52.dp),
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
