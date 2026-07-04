package com.pact.app.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pact.app.R
import com.pact.app.core.Apps
import com.pact.app.core.PactState
import com.pact.app.service.BlockerService
import com.pact.app.ui.theme.Amber
import com.pact.app.ui.theme.CardBorder
import com.pact.app.ui.theme.Mint
import com.pact.app.ui.theme.Periwinkle
import com.pact.app.ui.theme.Surface1
import com.pact.app.ui.theme.Surface2
import com.pact.app.ui.theme.TextSecondary
import com.pact.app.ui.theme.TextTertiary
import com.pact.app.ui.theme.Violet
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    state: PactState,
    onAddApps: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val snapshot by state.snapshot.collectAsState()
    val now by rememberNow()
    val serviceOn by produceState(initialValue = BlockerService.isEnabled(context)) {
        while (true) {
            value = BlockerService.isEnabled(context)
            delay(1500L)
        }
    }
    var appForAction by remember { mutableStateOf<String?>(null) }
    var verifyingRemoval by remember { mutableStateOf<String?>(null) }
    var verifyingUnlock by remember { mutableStateOf<String?>(null) }
    var choosingDuration by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        // header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 16.dp),
        ) {
            PactLogo(38)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(R.string.home_with, snapshot.guardianName),
                    style = MaterialTheme.typography.labelMedium,
                    color = Periwinkle,
                )
            }
            IconButton(onClick = onAddApps) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.home_add_apps), tint = TextSecondary)
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.home_settings), tint = TextSecondary)
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
        ) {
            // hero status
            item {
                HeroCard(
                    serviceOn = serviceOn,
                    lockedCount = snapshot.blocked.size,
                    interventionsToday = snapshot.blocksToday.values.sum(),
                    onEnable = {
                        context.startActivity(
                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    },
                )
            }

            // currently unlocked
            val unlockedNow = snapshot.unlockUntil.filter { it.value > now && it.key in snapshot.blocked }
            if (unlockedNow.isNotEmpty()) {
                item { SectionLabel(stringResource(R.string.section_on_break), Modifier.padding(top = 12.dp)) }
                items(unlockedNow.keys.sorted(), key = { "u_$it" }) { pkg ->
                    AppRow(
                        pkg = pkg,
                        subtitle = stringResource(
                            R.string.row_relocks_in,
                            formatCountdown((unlockedNow[pkg] ?: 0) - now),
                        ),
                        subtitleColor = Mint,
                        trailing = {
                            TextButton(onClick = { state.relock(pkg) }) {
                                Text(stringResource(R.string.action_relock), color = Periwinkle)
                            }
                        },
                    )
                }
            }

            // locked apps
            item { SectionLabel(stringResource(R.string.section_locked), Modifier.padding(top = 12.dp)) }
            val locked = snapshot.blocked
                .filter { (snapshot.unlockUntil[it] ?: 0L) <= now }
                .sortedBy { Apps.label(context, it).lowercase() }
            if (snapshot.blocked.isEmpty()) {
                item {
                    PactCard {
                        Text(
                            stringResource(R.string.home_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                    }
                }
            } else if (locked.isEmpty()) {
                item {
                    PactCard {
                        Text(
                            stringResource(R.string.home_all_on_break),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                    }
                }
            }
            items(locked, key = { "l_$it" }) { pkg ->
                val blocksToday = snapshot.blocksToday[pkg] ?: 0
                AppRow(
                    pkg = pkg,
                    subtitle = when {
                        blocksToday == 1 -> stringResource(R.string.row_blocked_once)
                        blocksToday > 1 -> stringResource(R.string.row_blocked_times, blocksToday)
                        else -> null
                    },
                    subtitleColor = TextTertiary,
                    onClick = { appForAction = pkg },
                    trailing = {
                        Icon(
                            Icons.Rounded.Lock,
                            contentDescription = stringResource(R.string.cd_locked),
                            tint = TextTertiary,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                )
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }

    // action dialog for a locked app
    appForAction?.let { pkg ->
        AlertDialog(
            onDismissRequest = { appForAction = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(Apps.label(context, pkg), style = MaterialTheme.typography.headlineSmall) },
            text = {
                Text(
                    stringResource(R.string.dialog_locked_body, snapshot.guardianName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = {
                        verifyingUnlock = pkg
                        appForAction = null
                    }) { Text(stringResource(R.string.action_unlock_a_while), color = Periwinkle) }
                    TextButton(onClick = {
                        verifyingRemoval = pkg
                        appForAction = null
                    }) { Text(stringResource(R.string.action_remove_from_pact), color = MaterialTheme.colorScheme.error) }
                }
            },
            dismissButton = {
                TextButton(onClick = { appForAction = null }) { Text(stringResource(R.string.common_close)) }
            },
        )
    }

    verifyingUnlock?.let { pkg ->
        VerifyCodeDialog(
            state = state,
            title = stringResource(R.string.unlock_q_title, Apps.label(context, pkg)),
            subtitle = stringResource(R.string.ask_code, snapshot.guardianName),
            onDismiss = { verifyingUnlock = null },
            onVerified = {
                verifyingUnlock = null
                choosingDuration = pkg
            },
        )
    }

    choosingDuration?.let { pkg ->
        AlertDialog(
            onDismissRequest = { choosingDuration = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(R.string.duration_title), style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(
                        R.string.duration_5m to 5 * 60_000L,
                        R.string.duration_15m to 15 * 60_000L,
                        R.string.duration_1h to 60 * 60_000L,
                        R.string.duration_midnight to PactState.untilMidnightMillis(),
                    ).forEach { (labelRes, duration) ->
                        PactButton(
                            stringResource(labelRes),
                            onClick = {
                                state.unlockFor(pkg, duration)
                                choosingDuration = null
                            },
                            tonal = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { choosingDuration = null }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }

    verifyingRemoval?.let { pkg ->
        VerifyCodeDialog(
            state = state,
            title = stringResource(R.string.remove_q_title, Apps.label(context, pkg)),
            subtitle = stringResource(R.string.remove_q_body, snapshot.guardianName),
            onDismiss = { verifyingRemoval = null },
            onVerified = {
                state.removeBlocked(pkg)
                verifyingRemoval = null
            },
        )
    }
}

/** Big Breezy-style status hero: shield state + stat tiles. */
@Composable
private fun HeroCard(
    serviceOn: Boolean,
    lockedCount: Int,
    interventionsToday: Int,
    onEnable: () -> Unit,
) {
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (serviceOn) Brush.linearGradient(
                    listOf(Surface2, Violet.copy(alpha = 0.25f))
                ) else Brush.linearGradient(listOf(Surface2, Amber.copy(alpha = 0.16f)))
            )
            .border(1.dp, CardBorder, shape)
            .padding(22.dp)
            .animateContentSize(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Surface1),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = if (serviceOn) Mint else Amber,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    stringResource(if (serviceOn) R.string.hero_active_title else R.string.hero_down_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (serviceOn) Mint else Amber,
                )
                Text(
                    stringResource(if (serviceOn) R.string.hero_active_sub else R.string.hero_down_sub),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        if (serviceOn) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    value = "$lockedCount",
                    label = stringResource(R.string.stat_locked_label),
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    value = "$interventionsToday",
                    label = stringResource(R.string.stat_saves_label),
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            PactButton(
                stringResource(R.string.hero_enable),
                onClick = onEnable,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Surface1.copy(alpha = 0.75f))
            .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(value, style = MaterialTheme.typography.displaySmall, color = Periwinkle)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}

@Composable
private fun AppRow(
    pkg: String,
    subtitle: String?,
    subtitleColor: androidx.compose.ui.graphics.Color,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(20.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Surface1)
            .border(1.dp, CardBorder, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        AppIconImage(remember(pkg) { Apps.icon(context, pkg) }, sizeDp = 42)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(Apps.label(context, pkg), style = MaterialTheme.typography.titleSmall)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = subtitleColor)
            }
        }
        trailing()
    }
}
