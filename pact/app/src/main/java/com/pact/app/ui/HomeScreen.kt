package com.pact.app.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pact.app.core.Apps
import com.pact.app.core.PactState
import com.pact.app.service.BlockerService
import com.pact.app.ui.theme.Amber
import com.pact.app.ui.theme.Ink
import com.pact.app.ui.theme.Mint
import com.pact.app.ui.theme.Periwinkle
import com.pact.app.ui.theme.Surface2
import com.pact.app.ui.theme.TextSecondary
import com.pact.app.ui.theme.TextTertiary
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        // header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 16.dp),
        ) {
            Icon(Icons.Rounded.Shield, contentDescription = null, tint = Periwinkle)
            Spacer(Modifier.width(10.dp))
            Text("Pact", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            IconButton(onClick = onAddApps) {
                Icon(Icons.Rounded.Add, contentDescription = "Add apps", tint = TextSecondary)
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = TextSecondary)
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
        ) {
            // status card
            item {
                if (!serviceOn) {
                    PactCard(background = Surface2) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Shield, contentDescription = null, tint = Amber)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("The shield is down", style = MaterialTheme.typography.titleSmall, color = Amber)
                                Text(
                                    "Nothing is being blocked. Turn the Pact shield back on in accessibility settings.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        PactButton(
                            "Raise the shield",
                            onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    PactCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(44.dp).clip(CircleShape).background(Surface2),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Rounded.Shield, contentDescription = null, tint = Mint)
                            }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text("Protection active", style = MaterialTheme.typography.titleSmall, color = Mint)
                                Text(
                                    "${snapshot.blocked.size} ${if (snapshot.blocked.size == 1) "app" else "apps"} in your Pact with ${snapshot.guardianName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                )
                            }
                        }
                    }
                }
            }

            // currently unlocked
            val unlockedNow = snapshot.unlockUntil.filter { it.value > now && it.key in snapshot.blocked }
            if (unlockedNow.isNotEmpty()) {
                item { SectionLabel("On a break", Modifier.padding(top = 10.dp)) }
                items(unlockedNow.keys.sorted(), key = { "u_$it" }) { pkg ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        AppIconImage(remember(pkg) { Apps.icon(context, pkg) }, sizeDp = 40)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(Apps.label(context, pkg), style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Relocks in ${formatCountdown((unlockedNow[pkg] ?: 0) - now)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Mint,
                            )
                        }
                        TextButton(onClick = { state.relock(pkg) }) {
                            Text("Relock now", color = Periwinkle)
                        }
                    }
                }
            }

            // locked apps
            item { SectionLabel("Locked", Modifier.padding(top = 10.dp)) }
            if (snapshot.blocked.isEmpty()) {
                item {
                    PactCard {
                        Text(
                            "No apps in your Pact yet. Tap + to lock the apps that pull you in.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                    }
                }
            }
            val locked = snapshot.blocked
                .filter { (snapshot.unlockUntil[it] ?: 0L) <= now }
                .sortedBy { Apps.label(context, it).lowercase() }
            items(locked, key = { "l_$it" }) { pkg ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { appForAction = pkg }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    AppIconImage(remember(pkg) { Apps.icon(context, pkg) }, sizeDp = 40)
                    Spacer(Modifier.width(14.dp))
                    Text(
                        Apps.label(context, pkg),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        Icons.Rounded.Lock,
                        contentDescription = "Locked",
                        tint = TextTertiary,
                        modifier = Modifier.size(20.dp),
                    )
                }
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
                    "This app is locked by your Pact. To use it, open it and enter a code from ${snapshot.guardianName}. To remove it from the Pact entirely, you'll also need a code.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    verifyingRemoval = pkg
                    appForAction = null
                }) { Text("Remove from Pact…", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { appForAction = null }) { Text("Close") }
            },
        )
    }

    verifyingRemoval?.let { pkg ->
        VerifyCodeDialog(
            state = state,
            title = "Remove ${Apps.label(context, pkg)}?",
            subtitle = "Ask ${snapshot.guardianName} for the current code to remove this app from your Pact.",
            onDismiss = { verifyingRemoval = null },
            onVerified = {
                state.removeBlocked(pkg)
                verifyingRemoval = null
            },
        )
    }
}
