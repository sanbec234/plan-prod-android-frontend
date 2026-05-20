package com.hanghub.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanghub.app.data.SampleData
import com.hanghub.app.ui.components.HHCard
import com.hanghub.app.ui.components.VibeChip
import com.hanghub.app.ui.theme.HHMotion
import com.hanghub.app.ui.theme.HHRadius
import com.hanghub.app.ui.theme.HHType
import com.hanghub.app.ui.theme.HangVibe
import com.hanghub.app.ui.theme.UserStatus
import com.hanghub.app.ui.theme.hh

// ═══════════════════════════════════════════════════════════════════════════
// MARK: ProfileScreen
// ═══════════════════════════════════════════════════════════════════════════

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun ProfileScreen() {
    val c = hh
    val me = SampleData.users[0]
    var selectedStatus by remember { mutableStateOf(UserStatus.FREE) }
    var discoveryRadius by remember { mutableFloatStateOf(2f) }
    var activeVibes by remember { mutableStateOf(setOf(HangVibe.COZY, HangVibe.CHILL, HangVibe.ADVENTUROUS, HangVibe.CULTURAL)) }
    val notifLabels = listOf("Friend nearby", "Plan invite", "Voting ends", "Cost settled", "Nudges", "Aura gain")
    var notifStates by remember { mutableStateOf(listOf(true, true, true, false, true, false)) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(c.bg),
        contentPadding = PaddingValues(bottom = 110.dp),
    ) {
        item { Spacer(Modifier.height(16.dp)); MonoLabel("Profile", Modifier.padding(horizontal = 18.dp)); Spacer(Modifier.height(12.dp)) }

        // ── Hero card ──────────────────────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .padding(horizontal = 18.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(HHRadius.xl))
                    .background(c.accentSoft)
                    .padding(20.dp)
            ) {
                // backdrop circle
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 40.dp, y = (-40).dp)
                        .clip(CircleShape)
                        .background(c.accent.copy(alpha = 0.12f))
                )
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    // avatar + name
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 18.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Box(modifier = Modifier.size(80.dp).clip(CircleShape).border(3.dp, c.statusFree, CircleShape))
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(74.dp).clip(CircleShape).background(c.surface)
                            ) { Text(me.avatar, fontSize = 38.sp) }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(me.name, style = HHType.display(26), color = c.accentInk)
                            Text("@you · free now", style = HHType.bodySm, color = c.accentInk.copy(alpha = 0.75f))
                        }
                    }
                    // stat tiles
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf("AURA" to "✦ ${me.aura}", "STREAK" to "🔥 ${me.streak}", "HANGS" to "47").forEach { (label, value) ->
                            Column(
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(HHRadius.md)).background(c.surface).padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                MonoLabel(label)
                                Text(value, style = HHType.display(22), color = c.ink)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(0.dp))
        }

        // ── My vibes ───────────────────────────────────────────────────
        item {
            Column(modifier = Modifier.padding(horizontal = 18.dp).padding(top = 24.dp)) {
                MonoLabel("My vibes")
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HangVibe.entries.forEach { vibe ->
                        VibeChip(
                            vibe = vibe,
                            selected = activeVibes.contains(vibe),
                            compact = true,
                            onClick = {
                                activeVibes = if (activeVibes.contains(vibe)) activeVibes - vibe else activeVibes + vibe
                            }
                        )
                    }
                }
            }
        }

        // ── Controls header ────────────────────────────────────────────
        item {
            Column(modifier = Modifier.padding(horizontal = 18.dp).padding(top = 26.dp)) {
                MonoLabel("Controls")
                Text("Tune your HangHub.", style = HHType.display(19), color = c.ink)
            }
        }

        // ── Discovery radius ───────────────────────────────────────────
        item {
            HHCard(
                modifier = Modifier.padding(horizontal = 18.dp).padding(top = 10.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            MonoLabel("Discovery radius")
                            Text("${discoveryRadius.toInt()} mi", style = HHType.display(28), color = c.ink)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("12", style = HHType.bodyMd, fontWeight = FontWeight.Bold, color = c.ink)
                            Text("friends\nin range", style = HHType.caption, color = c.inkMute)
                        }
                    }
                    Slider(
                        value = discoveryRadius,
                        onValueChange = { discoveryRadius = it },
                        valueRange = 0.5f..10f,
                        colors = SliderDefaults.colors(thumbColor = c.accent, activeTrackColor = c.accent, inactiveTrackColor = c.bgElev),
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("0.5mi","1mi","2mi","5mi","10mi").forEach { label ->
                            Text(label, style = HHType.monoXs, color = c.inkDim)
                        }
                    }
                }
            }
        }

        // ── Status picker ──────────────────────────────────────────────
        item {
            HHCard(modifier = Modifier.padding(horizontal = 18.dp).padding(top = 10.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MonoLabel("Status")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(UserStatus.FREE, UserStatus.BUSY, UserStatus.OFFLINE).forEach { status ->
                            val active = selectedStatus == status
                            val color  = status.color(c.isDark)
                            Surface(
                                onClick = { selectedStatus = status },
                                shape = RoundedCornerShape(HHRadius.md),
                                color = if (active) color.copy(alpha = 0.15f) else c.bgElev,
                                border = BorderStroke(if (active) 1.5.dp else 1.dp, if (active) color else c.stroke),
                                modifier = Modifier.weight(1f).height(56.dp),
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
                                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
                                    Spacer(Modifier.height(4.dp))
                                    Text(status.name.lowercase().replaceFirstChar { it.uppercase() }, style = HHType.caption, fontWeight = FontWeight.Bold, color = c.ink)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Notification chips ─────────────────────────────────────────
        item {
            HHCard(modifier = Modifier.padding(horizontal = 18.dp).padding(top = 10.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MonoLabel("Ping me when")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        notifLabels.forEachIndexed { i, label ->
                            val on = notifStates[i]
                            Surface(
                                onClick = {
                                    notifStates = notifStates.toMutableList().also { it[i] = !it[i] }
                                },
                                shape = CircleShape,
                                color = if (on) c.accent else c.bgElev,
                                border = BorderStroke(1.dp, if (on) Color.Transparent else c.stroke),
                            ) {
                                Text(
                                    if (on) "✓ $label" else label,
                                    style = HHType.caption,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (on) c.onAccent else c.inkMute,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Aura breakdown ─────────────────────────────────────────────
        item {
            HHCard(modifier = Modifier.padding(horizontal = 18.dp).padding(top = 10.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        MonoLabel("Aura sources")
                        Text("✦ ${me.aura} / 100", style = HHType.mono, color = c.accentInk)
                    }
                    listOf(Triple("Hosting",42, 32), Triple("Attending",36,28), Triple("Planning",22,18)).forEach { (key, pct, value) ->
                        val animPct by animateFloatAsState(pct / 100f, HHMotion.subtle(), label = "aura_$key")
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(key, style = HHType.bodySm, fontWeight = FontWeight.SemiBold, color = c.ink, modifier = Modifier.width(70.dp))
                            Box(modifier = Modifier.weight(1f).height(8.dp).clip(CircleShape).background(c.bgElev)) {
                                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(animPct).background(c.accent).clip(CircleShape))
                            }
                            Text("$value", style = HHType.mono, fontWeight = FontWeight.Bold, color = c.ink, modifier = Modifier.width(28.dp))
                        }
                    }
                }
            }
        }

        // ── Settings list ──────────────────────────────────────────────
        item {
            val rows = listOf(
                Triple("Appearance",    "Auto",         false),
                Triple("Privacy",       "Friends only", false),
                Triple("History",       "47 hangs",     false),
                Triple("Tickets",       "2 upcoming",   false),
                Triple("Help & feedback","",            false),
                Triple("Sign out",      "",             true),
            )
            Surface(
                shape = RoundedCornerShape(HHRadius.lg),
                color = c.surface,
                border = BorderStroke(1.dp, c.stroke),
                modifier = Modifier.padding(horizontal = 18.dp).padding(top = 18.dp).fillMaxWidth(),
            ) {
                Column {
                    rows.forEachIndexed { i, (label, value, danger) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {}.padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(label, style = HHType.bodyMd, fontWeight = FontWeight.Medium, color = if (danger) Color(0xFFE2442F) else c.ink)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (value.isNotEmpty()) Text(value, style = HHType.caption, color = c.inkMute)
                                if (!danger) Text("›", fontSize = 18.sp, color = c.inkDim)
                            }
                        }
                        if (i < rows.size - 1) HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = c.stroke)
                    }
                }
            }
        }
    }
}

// Overloaded MonoLabel with modifier
@Composable
private fun MonoLabel(text: String, modifier: Modifier = Modifier) {
    val c = hh
    Text(text.uppercase(), style = HHType.monoXs, color = c.inkDim, letterSpacing = 1.sp, modifier = modifier)
}
