package com.hanghub.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.hanghub.app.ToastData
import com.hanghub.app.data.HHPlan
import com.hanghub.app.data.PlanState
import com.hanghub.app.data.SampleData
import com.hanghub.app.ui.components.AvatarView
import com.hanghub.app.ui.components.HHCard
import com.hanghub.app.ui.components.LiveDot
import com.hanghub.app.ui.components.PlanCard
import com.hanghub.app.ui.chrome.LocalAppChrome
import com.hanghub.app.ui.theme.HHRadius
import com.hanghub.app.ui.theme.HHType
import com.hanghub.app.ui.theme.hh

// ═══════════════════════════════════════════════════════════════════════════
// MARK: PlansScreen
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun PlansScreen(onCreatePlan: () -> Unit, showToast: (ToastData) -> Unit) {
    val c = hh
    var selectedPlan by remember { mutableStateOf<HHPlan?>(null) }

    val activePlans   = SampleData.plans.filter { it.inHrs < 24 }
    val upcomingPlans = SampleData.plans.filter { it.inHrs >= 24 }

    Box(modifier = Modifier.fillMaxSize().background(c.bg)) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            // ── Header ─────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 18.dp, end = 18.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        MonoLabel("Hangouts")
                        Text("Your week", style = HHType.display(30), color = c.ink)
                    }
                    Surface(
                        onClick = onCreatePlan,
                        shape = CircleShape,
                        color = c.accent,
                        shadowElevation = 8.dp,
                        modifier = Modifier.size(44.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("+", fontSize = 22.sp, color = c.onAccent, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ── Happening soon ─────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp).padding(top = 22.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MonoLabel("Happening soon · ${activePlans.size}")
                    LiveDot(color = c.statusFree, label = "LIVE")
                }
            }
            items(activePlans) { plan ->
                PlanCard(plan = plan, onClick = { selectedPlan = plan })
                Spacer(Modifier.height(12.dp).padding(horizontal = 18.dp))
            }

            // ── This week ──────────────────────────────────────────────
            item {
                MonoLabel(
                    "This week · ${upcomingPlans.size}",
                    modifier = Modifier.padding(horizontal = 18.dp).padding(top = 22.dp, bottom = 12.dp)
                )
            }
            items(upcomingPlans) { plan ->
                Box(modifier = Modifier.padding(horizontal = 18.dp)) {
                    PlanCard(plan = plan, onClick = { selectedPlan = plan })
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    // Plan detail sheet
    selectedPlan?.let { plan ->
        PlanDetailSheet(plan = plan, onDismiss = { selectedPlan = null })
    }
}

// Overloaded MonoLabel with modifier
@Composable
private fun MonoLabel(text: String, modifier: Modifier = Modifier) {
    val c = hh
    Text(
        text.uppercase(),
        style = HHType.monoXs,
        color = c.inkDim,
        letterSpacing = 1.sp,
        modifier = modifier,
    )
}

// ═══════════════════════════════════════════════════════════════════════════
// MARK: PlanDetailSheet
// ═══════════════════════════════════════════════════════════════════════════

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PlanDetailSheet(plan: HHPlan, onDismiss: () -> Unit) {
    val c = hh
    val dark = c.isDark
    val chrome = LocalAppChrome.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var myRsvp by remember { mutableStateOf(plan.rsvp["me"] ?: "yes") }

    // Hide the floating tab bar while this sheet is shown
    DisposableEffect(Unit) {
        val pop = chrome.pushTabBarHidden()
        onDispose { pop() }
    }

    LaunchedEffect(Unit) {
        sheetState.expand()
    }

    val participants = plan.participantIDs.mapNotNull { SampleData.user(it) }
    val allRsvp = plan.rsvp.toMutableMap().also { it["me"] = myRsvp }
    val yesCount   = allRsvp.values.count { it == "yes" }
    val maybeCount = allRsvp.values.count { it == "maybe" }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = c.bg,
        dragHandle = {
            Box(modifier = Modifier.padding(vertical = 12.dp)) {
                Box(modifier = Modifier.width(40.dp).height(4.dp).clip(CircleShape).background(c.strokeHi))
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 120.dp),
            ) {
                // ── Vibe header ────────────────────────────────────────
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(plan.vibe.bg(dark))
                            .padding(18.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                            Text(
                                if (plan.state == PlanState.VOTING) "Voting · ${plan.time}" else "Confirmed · ${plan.time}",
                                style = HHType.monoXs,
                                color = plan.vibe.ink(dark).copy(alpha = 0.75f),
                                modifier = Modifier.padding(bottom = 18.dp)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(plan.vibe.emoji, fontSize = 44.sp)
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(plan.title, style = HHType.display(28), color = plan.vibe.ink(dark), maxLines = 2)
                                    plan.place?.let { place ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text("📍", fontSize = 11.sp)
                                            Text(place.name, style = HHType.bodySm, color = plan.vibe.ink(dark).copy(alpha = 0.75f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Participants ───────────────────────────────────────
                item {
                    Column(modifier = Modifier.padding(horizontal = 18.dp).padding(top = 22.dp)) {
                        com.hanghub.app.ui.components.MonoLabel(
                            text = "${participants.size} in the group · $yesCount going${if (maybeCount > 0) " · $maybeCount maybe" else ""}"
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            participants.forEach { user ->
                                val rsvp = if (user.id == "me") myRsvp else (plan.rsvp[user.id] ?: "waiting")
                                val rsvpColor = when (rsvp) {
                                    "yes"   -> c.statusFree
                                    "maybe" -> c.statusBusy
                                    "no"    -> Color(0xFFE2442F)
                                    else    -> c.inkDim
                                }
                                HHCard(modifier = Modifier.width(68.dp), padding = PaddingValues(10.dp)) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        AvatarView(user = user, size = 38.dp)
                                        Text(
                                            user.name.split(" ").first(),
                                            style = HHType.caption,
                                            fontWeight = FontWeight.SemiBold,
                                            color = c.ink,
                                        )
                                        Surface(shape = CircleShape, color = rsvpColor.copy(alpha = 0.15f)) {
                                            Text(rsvp, style = HHType.monoXs, color = rsvpColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Voting ─────────────────────────────────────────────
                if (plan.state == PlanState.VOTING && plan.options.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 18.dp).padding(top = 22.dp)) {
                            com.hanghub.app.ui.components.MonoLabel("Vote · where to?")
                            Spacer(Modifier.height(10.dp))
                            plan.options.forEach { option ->
                                val voteIDs = plan.votes[option.id] ?: emptyList()
                                val pct = if (plan.participantIDs.isEmpty()) 0f
                                          else voteIDs.size.toFloat() / plan.participantIDs.size
                                val myVote = voteIDs.contains("me")
                                val animPct by animateFloatAsState(pct, spring(dampingRatio = 0.72f, stiffness = 260f), label = "pct")

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 10.dp)
                                        .clip(RoundedCornerShape(HHRadius.lg))
                                        .background(c.surface)
                                        .border(
                                            if (myVote) 1.5.dp else 1.dp,
                                            if (myVote) c.accent else c.stroke,
                                            RoundedCornerShape(HHRadius.lg)
                                        )
                                ) {
                                    // progress bar
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(animPct)
                                            .background(c.accentSoft.copy(alpha = 0.5f))
                                    )
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(option.emoji, fontSize = 26.sp)
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(option.name, style = HHType.bodyMd, fontWeight = FontWeight.Bold, color = c.ink)
                                            Text("${option.category} · ${String.format("%.1f", option.dist)}mi", style = HHType.caption, color = c.inkMute)
                                        }
                                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text("${(pct * 100).toInt()}%", style = HHType.mono, fontWeight = FontWeight.Bold, color = c.ink)
                                            Text("${voteIDs.size} vote${if (voteIDs.size != 1) "s" else ""}", style = HHType.caption, color = c.inkMute)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Cost split ─────────────────────────────────────────
                plan.cost?.let { cost ->
                    item {
                        Column(modifier = Modifier.padding(horizontal = 18.dp).padding(top = 22.dp)) {
                            com.hanghub.app.ui.components.MonoLabel("Cost split")
                            Spacer(Modifier.height(10.dp))
                            HHCard {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        Text("$${cost.total}", style = HHType.display(26), color = c.ink)
                                        Text("$${cost.perPerson} / person", style = HHType.caption, color = c.inkMute)
                                    }
                                    LinearProgressIndicator(
                                        progress = { cost.settled.toFloat() / plan.participantIDs.size },
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                        color = c.statusFree,
                                        trackColor = c.bgElev,
                                    )
                                    Text("${cost.settled} of ${plan.participantIDs.size} settled", style = HHType.caption, color = c.inkMute)
                                }
                            }
                        }
                    }
                }
            }

            // ── Sticky RSVP bar ────────────────────────────────────────
            Surface(
                color = c.bg,
                border = BorderStroke(1.dp, c.stroke),
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        Triple("no",    "Can't",  Color(0xFFE2442F)),
                        Triple("maybe", "Maybe",  c.statusBusy),
                        Triple("yes",   "I'm in", c.statusFree),
                    ).forEach { (key, label, color) ->
                        val isActive = myRsvp == key
                        Surface(
                            onClick = { myRsvp = key },
                            shape = CircleShape,
                            color = if (isActive) color else c.surface,
                            border = BorderStroke(if (isActive) 0.dp else 1.5.dp, if (isActive) Color.Transparent else c.stroke),
                            modifier = if (key == "yes") Modifier.weight(2f).height(52.dp)
                                       else Modifier.width(80.dp).height(52.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    label,
                                    style = HHType.bodyMd,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActive) Color.White else c.ink,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
