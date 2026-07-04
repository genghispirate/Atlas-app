package com.pact.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Group
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.core.content.ContextCompat
import com.pact.app.R
import com.pact.app.core.Apps
import com.pact.app.core.PactState
import com.pact.app.core.TrustNetwork
import com.pact.app.service.BlockerService
import com.pact.app.ui.theme.Amber
import com.pact.app.ui.theme.CardBorder
import com.pact.app.ui.theme.Mint
import com.pact.app.ui.theme.Periwinkle
import com.pact.app.ui.theme.Rose
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
    onOpenStats: () -> Unit,
    onOpenCircle: () -> Unit,
) {
    val context = LocalContext.current
    val network = remember { TrustNetwork.get(context) }
    val snapshot by state.snapshot.collectAsState()
    val netSnap by network.snapshot.collectAsState()
    val now by rememberNow()
    val serviceOn by produceState(initialValue = BlockerService.isEnabled(context)) {
        while (true) {
            value = BlockerService.isEnabled(context)
            delay(1500L)
        }
    }
    var appForAction by remember { mutableStateOf<String?>(null) }
    var changeRequested by remember { mutableStateOf(false) }
    val hasCircle = netSnap.approvers().isNotEmpty()

    // Break/shield notifications are optional; ask once on Android 13+.
    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

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
                    if (hasCircle) stringResource(R.string.circle_members, netSnap.supporters().size)
                    else stringResource(R.string.app_name),
                    style = MaterialTheme.typography.labelMedium,
                    color = Periwinkle,
                )
            }
            IconButton(onClick = onOpenCircle) {
                Icon(Icons.Rounded.Group, contentDescription = stringResource(R.string.circle_title), tint = TextSecondary)
            }
            IconButton(onClick = onOpenStats) {
                Icon(Icons.Rounded.BarChart, contentDescription = stringResource(R.string.stats_open), tint = TextSecondary)
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
                    interventionsToday = snapshot.today.blocks,
                    onEnable = {
                        context.startActivity(
                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    },
                )
            }

            // circle empty → gentle nudge (red locks fall back to a pause)
            if (!hasCircle) {
                item {
                    PactCard(
                        background = Surface2,
                        modifier = Modifier.padding(top = 4.dp).clickable(onClick = onOpenCircle),
                    ) {
                        Text(
                            stringResource(R.string.circle_needs_people),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Amber,
                        )
                    }
                }
            }

            // reflection: one gentle question after a break ends
            val pending = snapshot.pendingReflection(now)
            if (pending != null) {
                item {
                    PactCard(background = Surface2) {
                        Text(
                            stringResource(R.string.reflect_title, Apps.label(context, pending.pkg)),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            stringResource(R.string.reflect_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PactButton(
                                stringResource(R.string.reflect_yes),
                                onClick = { state.setWorthIt(pending.at, true) },
                                tonal = true,
                                modifier = Modifier.weight(1f),
                            )
                            PactButton(
                                stringResource(R.string.reflect_no),
                                onClick = { state.setWorthIt(pending.at, false) },
                                tonal = true,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        TextButton(onClick = { state.setWorthIt(pending.at, null) }) {
                            Text(stringResource(R.string.reflect_skip), color = TextTertiary)
                        }
                    }
                }
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
                val blocksToday = snapshot.today.blocksPerApp[pkg] ?: 0
                val tier = snapshot.tierOf(pkg)
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
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (tier == PactState.Tier.RED) Rose else Amber)
                        )
                        Spacer(Modifier.width(10.dp))
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

    // action dialog for a locked app: tier control + circle-gated changes
    appForAction?.let { pkg ->
        val tier = snapshot.tierOf(pkg)
        AlertDialog(
            onDismissRequest = { appForAction = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(Apps.label(context, pkg), style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column {
                    SectionLabel(stringResource(R.string.tier_label))
                    Spacer(Modifier.height(8.dp))
                    TierOption(
                        selected = tier == PactState.Tier.RED,
                        dotColor = Rose,
                        title = stringResource(R.string.tier_red),
                        body = stringResource(R.string.tier_red_desc),
                        onClick = { if (tier != PactState.Tier.RED) state.setTier(pkg, PactState.Tier.RED) },
                    )
                    Spacer(Modifier.height(8.dp))
                    TierOption(
                        selected = tier == PactState.Tier.YELLOW,
                        dotColor = Amber,
                        title = stringResource(R.string.tier_yellow),
                        body = stringResource(R.string.tier_yellow_desc),
                        onClick = {
                            // relaxing a red lock is a change the circle must approve
                            if (tier == PactState.Tier.RED && hasCircle) {
                                network.createRequest(
                                    kind = TrustNetwork.RequestKind.CHANGE,
                                    changeAction = TrustNetwork.CHANGE_TIER_DOWN,
                                    pkg = pkg,
                                    label = Apps.label(context, pkg),
                                    minutes = 0,
                                    reason = null,
                                    usageNote = null,
                                )
                                changeRequested = true
                                appForAction = null
                            } else if (tier == PactState.Tier.RED && !hasCircle) {
                                state.setTier(pkg, PactState.Tier.YELLOW)
                            }
                        },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (hasCircle) {
                        network.createRequest(
                            kind = TrustNetwork.RequestKind.CHANGE,
                            changeAction = TrustNetwork.CHANGE_REMOVE_APP,
                            pkg = pkg,
                            label = Apps.label(context, pkg),
                            minutes = 0,
                            reason = null,
                            usageNote = null,
                        )
                        changeRequested = true
                    } else {
                        state.removeBlocked(pkg)
                    }
                    appForAction = null
                }) { Text(stringResource(R.string.action_remove_from_pact), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { appForAction = null }) { Text(stringResource(R.string.common_close)) }
            },
        )
    }

    if (changeRequested) {
        AlertDialog(
            onDismissRequest = { changeRequested = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(R.string.request_sent), style = MaterialTheme.typography.headlineSmall) },
            text = {
                Text(
                    stringResource(R.string.request_change_sent),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = { changeRequested = false }) { Text(stringResource(R.string.common_done)) }
            },
        )
    }
}

@Composable
private fun TierOption(
    selected: Boolean,
    dotColor: androidx.compose.ui.graphics.Color,
    title: String,
    body: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) Surface2 else MaterialTheme.colorScheme.surface)
            .border(1.dp, if (selected) dotColor.copy(alpha = 0.6f) else CardBorder, shape)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Box(
            Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
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
