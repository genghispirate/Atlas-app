package com.pact.app.ui

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pact.app.R
import com.pact.app.core.PactState
import com.pact.app.core.Qr
import com.pact.app.core.Totp
import com.pact.app.service.BlockerService
import com.pact.app.ui.theme.Amber
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
 * Onboarding: a real introduction first (3 swipeable pages), then a role
 * choice — block my own apps, or hold the keys as a sponsor — then the
 * matching guided setup. The TOTP secret lives only in memory until setup
 * completes.
 */

private enum class Phase { Intro, RoleSelect, UserSetup, SponsorSetup }

@Composable
fun OnboardingFlow(state: PactState, onDone: () -> Unit) {
    var phase by remember { mutableStateOf(Phase.Intro) }
    BackHandler(enabled = phase == Phase.RoleSelect) { phase = Phase.Intro }
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
    val title: Int,
    val body: Int,
)

@Composable
private fun IntroPager(onFinished: () -> Unit) {
    val pages = listOf(
        IntroPage({ ArtLockedPhone() }, R.string.intro_page1_title, R.string.intro_page1_body),
        IntroPage({ ArtTwoPeople() }, R.string.intro_page2_title, R.string.intro_page2_body),
        IntroPage({ ArtOfflineShield() }, R.string.intro_page3_title, R.string.intro_page3_body),
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
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.weight(1f))
            if (pagerState.currentPage < pages.size - 1) {
                TextButton(onClick = onFinished) {
                    Text(stringResource(R.string.common_skip), color = TextTertiary)
                }
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
                Text(
                    stringResource(p.title),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    stringResource(p.body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }

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
            text = stringResource(
                if (pagerState.currentPage == pages.size - 1) R.string.intro_get_started
                else R.string.common_next
            ),
            onClick = {
                if (pagerState.currentPage == pages.size - 1) onFinished()
                else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            },
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
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
        Text(
            stringResource(R.string.role_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.role_body),
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(36.dp))

        RoleCard(
            icon = Icons.Rounded.Lock,
            iconOnGradient = true,
            title = stringResource(R.string.role_user_title),
            body = stringResource(R.string.role_user_body),
            onClick = onUser,
        )
        Spacer(Modifier.height(14.dp))
        RoleCard(
            icon = Icons.Rounded.Key,
            iconOnGradient = false,
            title = stringResource(R.string.role_sponsor_title),
            body = stringResource(R.string.role_sponsor_body),
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
            .border(1.dp, com.pact.app.ui.theme.CardBorder, shape)
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

    // system back walks one step backwards through setup
    BackHandler(enabled = step > 0) { step -= 1 }

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
            .imePadding()
            .navigationBarsPadding()
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
        title = stringResource(R.string.guardian_title),
        subtitle = stringResource(R.string.guardian_body),
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.guardian_name_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                if (name.trim().length >= 2) onNext()
            }),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Periwinkle,
                unfocusedBorderColor = Surface2,
            ),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.guardian_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = TextTertiary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        PactButton(
            stringResource(R.string.common_continue),
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
        title = stringResource(R.string.pair_title),
        subtitle = stringResource(R.string.pair_body, guardianName),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(16.dp),
        ) {
            Image(
                bitmap = qr,
                contentDescription = stringResource(R.string.pair_qr_desc),
                modifier = Modifier.size(220.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        PactCard {
            Text(
                stringResource(R.string.pair_on_their_phone, guardianName),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(12.dp))
            NumberedStep(1, stringResource(R.string.pair_step1))
            Spacer(Modifier.height(10.dp))
            NumberedStep(2, stringResource(R.string.pair_step2))
            Spacer(Modifier.height(10.dp))
            NumberedStep(3, stringResource(R.string.pair_step3))
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(R.string.pair_alt_authenticator, Totp.prettySecret(secret)),
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
                    stringResource(R.string.pair_warning),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        PactButton(
            stringResource(R.string.pair_confirm, guardianName),
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
        title = stringResource(R.string.prove_title),
        subtitle = stringResource(R.string.prove_body, guardianName),
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
            autoFocus = true,
        )
        if (error) {
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.prove_error),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(20.dp))
        TextButton(onClick = onBack) {
            Text(stringResource(R.string.prove_show_qr), color = TextSecondary)
        }
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
        title = stringResource(R.string.perm_title),
        subtitle = stringResource(R.string.perm_body),
    ) {
        PactCard {
            NumberedStep(1, stringResource(R.string.perm_step1))
            Spacer(Modifier.height(12.dp))
            NumberedStep(2, stringResource(R.string.perm_step2))
            Spacer(Modifier.height(12.dp))
            NumberedStep(3, stringResource(R.string.perm_step3))
            Spacer(Modifier.height(12.dp))
            NumberedStep(4, stringResource(R.string.perm_step4))
            Spacer(Modifier.height(12.dp))
            NumberedStep(5, stringResource(R.string.perm_step5))
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
                    stringResource(
                        if (serviceOn) R.string.perm_status_on else R.string.perm_status_waiting
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (serviceOn) Mint else TextSecondary,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        if (serviceOn) {
            PactButton(
                stringResource(R.string.common_continue),
                onClick = onNext,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            PactButton(
                stringResource(R.string.perm_open_settings),
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onNext) {
                Text(stringResource(R.string.perm_later), color = TextTertiary)
            }
        }
    }
}

@Composable
private fun PickAppsStep(
    selected: Set<String>,
    onSelectedChange: (Set<String>) -> Unit,
    onNext: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            stringResource(R.string.pick_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        )
        Box(Modifier.weight(1f)) {
            AppPickerList(selected = selected, onSelectedChange = onSelectedChange)
        }
        PactButton(
            if (selected.isEmpty()) stringResource(R.string.pick_min_one)
            else pluralStringResource(R.plurals.lock_n_apps, selected.size, selected.size),
            onClick = onNext,
            enabled = selected.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        )
    }
}

@Composable
private fun SealStep(guardianName: String, onFinish: () -> Unit) {
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
        Text(stringResource(R.string.seal_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(14.dp))
        Text(
            stringResource(R.string.seal_body, guardianName),
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(40.dp))
        PactButton(
            stringResource(R.string.seal_begin),
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
