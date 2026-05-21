package com.hanghub.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hanghub.app.ToastData
import com.hanghub.app.core.appContainer
import com.hanghub.app.core.viewModelFactory
import com.hanghub.app.data.HHPlan
import com.hanghub.app.data.HHUser
import com.hanghub.app.data.PlanStage
import com.hanghub.app.data.PlanState
import com.hanghub.app.data.toHHUser
import com.hanghub.app.ui.components.AvatarView
import com.hanghub.app.ui.components.HHCard
import com.hanghub.app.ui.components.LiveDot
import com.hanghub.app.ui.components.PlanCard
import com.hanghub.app.ui.chrome.LocalAppChrome
import com.hanghub.app.ui.state.AppStateViewModel
import com.hanghub.app.ui.state.LocalAppState
import com.hanghub.app.ui.theme.HHRadius
import com.hanghub.app.ui.theme.HHType
import com.hanghub.app.ui.theme.UserStatus
import com.hanghub.app.ui.theme.hh

// ═══════════════════════════════════════════════════════════════════════════
// MARK: PlansScreen — wired to live backend plans via AppStateViewModel
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun PlansScreen(onCreatePlan: () -> Unit, showToast: (ToastData) -> Unit) {
    val c = hh
    val appState = LocalAppState.current
    val container = appContainer()
    val plansVm: PlansViewModel = viewModel(
        factory = viewModelFactory {
            PlansViewModel(appState, container.planRepository, container.chatRepository)
        }
    )
    var selectedPlanId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(plansVm.actionError) {
        plansVm.actionError?.let {
            showToast(ToastData(it))
            plansVm.clearError()
        }
    }

    val plans = appState.plans
    val activePlans = plans.filter { it.inHrs < 24 }
    val upcomingPlans = plans.filter { it.inHrs >= 24 }

    Box(modifier = Modifier.fillMaxSize().background(c.bg)) {
        when {
            appState.isLoadingPlans && plans.isEmpty() -> LoadingState()
            appState.loadError != null && plans.isEmpty() ->
                ErrorState(message = appState.loadError!!, onRetry = { appState.loadInitialData() })
            plans.isEmpty() -> EmptyState(onCreatePlan)
            else -> LazyColumn(
                contentPadding = PaddingValues(bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                item { PlansHeader(onCreatePlan) }

                item {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp).padding(top = 22.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MonoLabel("Happening soon · ${activePlans.size}")
                        if (activePlans.isNotEmpty()) LiveDot(color = c.statusFree, label = "LIVE")
                    }
                }
                items(activePlans, key = { it.id }) { plan ->
                    Box(modifier = Modifier.padding(horizontal = 18.dp)) {
                        PlanCard(plan = plan, onClick = { selectedPlanId = plan.id })
                    }
                    Spacer(Modifier.height(12.dp))
                }

                item {
                    MonoLabel(
                        "This week · ${upcomingPlans.size}",
                        modifier = Modifier.padding(horizontal = 18.dp).padding(top = 22.dp, bottom = 12.dp),
                    )
                }
                items(upcomingPlans, key = { it.id }) { plan ->
                    Box(modifier = Modifier.padding(horizontal = 18.dp)) {
                        PlanCard(plan = plan, onClick = { selectedPlanId = plan.id })
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }

    selectedPlanId?.let { id ->
        PlanDetailSheet(
            planId = id,
            plansVm = plansVm,
            onDismiss = { selectedPlanId = null },
        )
    }
}

@Composable
private fun PlansHeader(onCreatePlan: () -> Unit) {
    val c = hh
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, start = 18.dp, end = 18.dp, bottom = 6.dp),
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

@Composable
private fun LoadingState() {
    val c = hh
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = c.accent)
        Spacer(Modifier.height(12.dp))
        Text("Loading your hangouts…", style = HHType.bodySm, color = c.inkMute)
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    val c = hh
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("⚠️", fontSize = 32.sp)
        Spacer(Modifier.height(10.dp))
        Text(message, style = HHType.bodySm, color = c.inkMute, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Surface(onClick = onRetry, shape = CircleShape, color = c.accent) {
            Text("Retry", style = HHType.bodySm, fontWeight = FontWeight.Bold, color = c.onAccent, modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp))
        }
    }
}

@Composable
private fun EmptyState(onCreatePlan: () -> Unit) {
    val c = hh
    Column(modifier = Modifier.fillMaxSize()) {
        PlansHeader(onCreatePlan)
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("📅", fontSize = 40.sp)
            Spacer(Modifier.height(12.dp))
            Text("No hangouts yet", style = HHType.display(20), color = c.ink)
            Spacer(Modifier.height(6.dp))
            Text("Start a plan and invite your circle.", style = HHType.bodySm, color = c.inkMute)
            Spacer(Modifier.height(18.dp))
            Surface(onClick = onCreatePlan, shape = CircleShape, color = c.accent) {
                Text("+ New hang", style = HHType.bodySm, fontWeight = FontWeight.Bold, color = c.onAccent, modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp))
            }
        }
    }
}

@Composable
private fun MonoLabel(text: String, modifier: Modifier = Modifier) {
    val c = hh
    Text(text.uppercase(), style = HHType.monoXs, color = c.inkDim, letterSpacing = 1.sp, modifier = modifier)
}

private fun resolveUser(id: String, appState: AppStateViewModel): HHUser {
    if (id == "me") {
        appState.currentUser?.let { return it.toHHUser() }
    }
    appState.friends.firstOrNull { it.id == id }?.let { return it }
    return HHUser(id = id, name = id.take(8), avatar = "👤", status = UserStatus.OFFLINE, aura = 0)
}

// ═══════════════════════════════════════════════════════════════════════════
// MARK: PlanDetailSheet — wired to PlansViewModel
// ═══════════════════════════════════════════════════════════════════════════

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PlanDetailSheet(planId: String, plansVm: PlansViewModel, onDismiss: () -> Unit) {
    val c = hh
    val dark = c.isDark
    val appState = LocalAppState.current
    val chrome = LocalAppChrome.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val plan: HHPlan = appState.plans.firstOrNull { it.id == planId } ?: run {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    DisposableEffectTabBar(chrome)
    LaunchedEffect(Unit) { sheetState.expand() }

    val participants = plan.participantIDs.map { resolveUser(it, appState) }
    val myRsvp = plan.rsvp["me"] ?: "yes"
    val yesCount = plan.rsvp.values.count { it == "yes" }
    val maybeCount = plan.rsvp.values.count { it == "maybe" }
    val isHost = plan.hostId == "me"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = c.bg,
        dragHandle = {
            Box(modifier = Modifier.padding(vertical = 12.dp)) {
                Box(modifier = Modifier.width(40.dp).height(4.dp).clip(CircleShape).background(c.strokeHi))
            }
        },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(contentPadding = PaddingValues(bottom = 130.dp)) {
                // ── Vibe header ──────────────────────────────────────────
                item {
                    Box(modifier = Modifier.fillMaxWidth().background(plan.vibe.bg(dark)).padding(18.dp)) {
                        Column {
                            Text(
                                if (plan.state == PlanState.VOTING) "Voting · ${plan.time}" else "Confirmed · ${plan.time}",
                                style = HHType.monoXs,
                                color = plan.vibe.ink(dark).copy(alpha = 0.75f),
                                modifier = Modifier.padding(bottom = 18.dp),
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
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

                // ── Participants ─────────────────────────────────────────
                item {
                    Column(modifier = Modifier.padding(horizontal = 18.dp).padding(top = 22.dp)) {
                        com.hanghub.app.ui.components.MonoLabel(
                            text = "${participants.size} in the group · $yesCount going" +
                                if (maybeCount > 0) " · $maybeCount maybe" else ""
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            participants.forEach { user ->
                                val rsvp = plan.rsvp[user.id] ?: "waiting"
                                val rsvpColor = when (rsvp) {
                                    "yes" -> c.statusFree
                                    "maybe" -> c.statusBusy
                                    "no" -> Color(0xFFE2442F)
                                    else -> c.inkDim
                                }
                                HHCard(modifier = Modifier.width(68.dp), padding = PaddingValues(10.dp)) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        AvatarView(user = user, size = 38.dp)
                                        Text(user.name.split(" ").first(), style = HHType.caption, fontWeight = FontWeight.SemiBold, color = c.ink, maxLines = 1)
                                        Surface(shape = CircleShape, color = rsvpColor.copy(alpha = 0.15f)) {
                                            Text(rsvp, style = HHType.monoXs, color = rsvpColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Voting ───────────────────────────────────────────────
                if (plan.stage == PlanStage.VOTING && plan.options.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 18.dp).padding(top = 22.dp)) {
                            com.hanghub.app.ui.components.MonoLabel("Vote · where to?")
                            Spacer(Modifier.height(10.dp))
                            plan.options.forEach { option ->
                                val voteIDs = plan.votes[option.id] ?: emptyList()
                                val myVote = appState.myVotes[plan.id] == option.id
                                val pct = if (plan.participantIDs.isEmpty()) 0f
                                else voteIDs.size.toFloat() / plan.participantIDs.size
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
                                            RoundedCornerShape(HHRadius.lg),
                                        )
                                        .clickable { plansVm.toggleVote(plan.id, option.id) },
                                ) {
                                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(animPct).background(c.accentSoft.copy(alpha = 0.5f)))
                                    Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(option.emoji, fontSize = 26.sp)
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(option.name, style = HHType.bodyMd, fontWeight = FontWeight.Bold, color = c.ink)
                                            Text(if (myVote) "Your vote" else "Tap to vote", style = HHType.caption, color = if (myVote) c.accent else c.inkMute)
                                        }
                                        Text("${voteIDs.size}", style = HHType.mono, fontWeight = FontWeight.Bold, color = c.ink)
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Cost split ───────────────────────────────────────────
                plan.cost?.let { cost ->
                    item {
                        Column(modifier = Modifier.padding(horizontal = 18.dp).padding(top = 22.dp)) {
                            com.hanghub.app.ui.components.MonoLabel("Cost split")
                            Spacer(Modifier.height(10.dp))
                            HHCard {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                                        Text("$${cost.total}", style = HHType.display(26), color = c.ink)
                                        Text("$${cost.perPerson} / person", style = HHType.caption, color = c.inkMute)
                                    }
                                    LinearProgressIndicator(
                                        progress = { if (plan.participantIDs.isEmpty()) 0f else cost.settled.toFloat() / plan.participantIDs.size },
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

                // ── Host controls ────────────────────────────────────────
                if (isHost) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 18.dp).padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            com.hanghub.app.ui.components.MonoLabel("Host controls")
                            if (plan.stage == PlanStage.SUGGESTION) {
                                HostButton("Open voting", c.accent, c.onAccent) {
                                    plansVm.transition(plan.id, "pulse_vote")
                                }
                            }
                            if (plan.stage == PlanStage.DECIDING) {
                                plan.options.forEach { option ->
                                    HostButton("Lock in ${option.name}", c.surface, c.ink) {
                                        plansVm.transition(plan.id, "locked_in", finalPlaceId = option.id)
                                    }
                                }
                            }
                            HostButton("Delete plan", c.surface, Color(0xFFE2442F)) {
                                plansVm.deletePlan(plan.id)
                                onDismiss()
                            }
                        }
                    }
                }
            }

            // ── Sticky RSVP bar ──────────────────────────────────────────
            Surface(
                color = c.bg,
                border = BorderStroke(1.dp, c.stroke),
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp).navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        Triple("no", "Can't", Color(0xFFE2442F)),
                        Triple("maybe", "Maybe", c.statusBusy),
                        Triple("yes", "I'm in", c.statusFree),
                    ).forEach { (key, label, color) ->
                        val isActive = myRsvp == key
                        Surface(
                            onClick = { plansVm.updateRsvp(plan.id, key) },
                            shape = CircleShape,
                            color = if (isActive) color else c.surface,
                            border = BorderStroke(if (isActive) 0.dp else 1.5.dp, if (isActive) Color.Transparent else c.stroke),
                            modifier = if (key == "yes") Modifier.weight(2f).height(52.dp) else Modifier.width(80.dp).height(52.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(label, style = HHType.bodyMd, fontWeight = FontWeight.Bold, color = if (isActive) Color.White else c.ink)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HostButton(label: String, bg: Color, fg: Color, onClick: () -> Unit) {
    val c = hh
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(HHRadius.md),
        color = bg,
        border = BorderStroke(1.dp, c.stroke),
        modifier = Modifier.fillMaxWidth().height(46.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = HHType.bodySm, fontWeight = FontWeight.Bold, color = fg)
        }
    }
}

@Composable
private fun DisposableEffectTabBar(chrome: com.hanghub.app.ui.chrome.AppChromeState) {
    androidx.compose.runtime.DisposableEffect(Unit) {
        val pop = chrome.pushTabBarHidden()
        onDispose { pop() }
    }
}
