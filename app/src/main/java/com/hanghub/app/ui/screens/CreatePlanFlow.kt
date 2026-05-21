package com.hanghub.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorizedAnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hanghub.app.core.appContainer
import com.hanghub.app.core.viewModelFactory
import com.hanghub.app.data.HHUser
import com.hanghub.app.data.SampleData
import com.hanghub.app.data.dto.ChatDto
import com.hanghub.app.ui.state.LocalAppState
import com.hanghub.app.ui.components.AvatarView
import com.hanghub.app.ui.components.HHButtonStyle
import com.hanghub.app.ui.components.HHCard
import com.hanghub.app.ui.components.MonoLabel
import com.hanghub.app.ui.components.VibeCard
import com.hanghub.app.ui.components.shimmer
import com.hanghub.app.ui.theme.HHMotion
import com.hanghub.app.ui.theme.HHRadius
import com.hanghub.app.ui.theme.HHType
import com.hanghub.app.ui.theme.HangVibe
import com.hanghub.app.ui.theme.hh
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

// ═══════════════════════════════════════════════════════════════════════════
// MARK: CreatePlanFlow
// ═══════════════════════════════════════════════════════════════════════════

data class CompanionSelection(
    val id: String,
    val name: String,
    val emoji: String?,
    val count: Int?,
    val isGroup: Boolean,
)

enum class DeadlinePreset(val label: String, val sub: String) {
    THIRTY_MIN("30 mins from now", "default — stay spontaneous"),
    ONE_HOUR(  "1 hour",           "give people a beat"),
    TWO_HOUR(  "2 hours",          "broader window"),
    NINE_PM(   "Tonight at 9 PM",  "evening hang"),
    NOON(      "Tomorrow at noon", "plan ahead"),
}

/** Resolve a preset to an absolute "voting closes" epoch-millis timestamp. */
private fun DeadlinePreset.voteUntilMillis(): Long {
    val now = System.currentTimeMillis()
    return when (this) {
        DeadlinePreset.THIRTY_MIN -> now + 30 * 60_000L
        DeadlinePreset.ONE_HOUR -> now + 60 * 60_000L
        DeadlinePreset.TWO_HOUR -> now + 120 * 60_000L
        DeadlinePreset.NINE_PM -> {
            val cal = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 21)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
            }
            if (cal.timeInMillis <= now) cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
            cal.timeInMillis
        }
        DeadlinePreset.NOON -> java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 12)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
        }.timeInMillis
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CreatePlanFlow(onDismiss: () -> Unit, onCreated: () -> Unit) {
    val c = hh
    val appState = LocalAppState.current
    val container = appContainer()
    val plansVm: PlansViewModel = viewModel(
        factory = viewModelFactory {
            PlansViewModel(appState, container.planRepository, container.chatRepository)
        }
    )
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var step by remember { mutableIntStateOf(0) }
    var title by remember { mutableStateOf("Cozy coffee") }
    var desc by remember { mutableStateOf("") }
    var companion by remember { mutableStateOf<CompanionSelection?>(null) }
    var vibe by remember { mutableStateOf(HangVibe.COZY) }
    var selectedPlaceIDs by remember { mutableStateOf(setOf("p1")) }
    var deadline by remember { mutableStateOf(DeadlinePreset.THIRTY_MIN) }
    var showSuccess by remember { mutableStateOf(false) }
    var loadingPlaces by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        sheetState.expand()
    }

    // trigger loading when entering step 3
    LaunchedEffect(step) {
        if (step == 3) {
            loadingPlaces = true
            delay(900)
            loadingPlaces = false
        }
    }

    // auto-dismiss after success
    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            delay(1800)
            onCreated()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = c.bg,
        modifier = Modifier.fillMaxSize(),
        dragHandle = null,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (showSuccess) {
                SuccessView(title = title, companion = companion, vibe = vibe)
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // ── Header ──────────────────────────────────────────
                    FlowHeader(step = step, totalSteps = 6, onBack = {
                        if (step > 0) step-- else onDismiss()
                    })

                    // ── Stage content ───────────────────────────────────
                    Box(modifier = Modifier.weight(1f)) {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 110.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            item {
                                AnimatedContent(
                                    targetState = step,
                                    transitionSpec = {
                                        if (targetState > initialState)
                                            slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                                        else
                                            slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                                    },
                                    label = "stage",
                                ) { currentStep ->
                                    when (currentStep) {
                                        0 -> Stage0Title(title, desc, onTitleChange = { title = it }, onDescChange = { desc = it })
                                        1 -> Stage1Companion(companion, appState.friends, appState.groups, onSelect = { companion = it })
                                        2 -> Stage2Vibe(vibe, onSelect = { vibe = it })
                                        3 -> Stage3Places(vibe, selectedPlaceIDs, loadingPlaces, onToggle = { id ->
                                            selectedPlaceIDs = if (selectedPlaceIDs.contains(id)) selectedPlaceIDs - id
                                            else if (selectedPlaceIDs.size < 3) selectedPlaceIDs + id
                                            else selectedPlaceIDs
                                        })
                                        4 -> Stage4Deadline(deadline, onSelect = { deadline = it })
                                        5 -> Stage5Review(title, vibe, companion, selectedPlaceIDs, deadline)
                                        else -> Spacer(Modifier.height(1.dp))
                                    }
                                }
                            }
                        }
                    }

                    // ── CTA ─────────────────────────────────────────────
                    Surface(color = c.bg, border = BorderStroke(0.dp, Color.Transparent)) {
                        Column {
                            HorizontalDivider(color = c.stroke)
                            HHButton(
                                label = when {
                                    plansVm.isCreating -> "Creating…"
                                    step == 5 -> "✓ Create plan"
                                    else -> "Continue →"
                                },
                                style = HHButtonStyle.PRIMARY,
                                fullWidth = true,
                                onClick = {
                                    if (!plansVm.isCreating) {
                                        if (step < 5) {
                                            step++
                                        } else {
                                            val comp = companion
                                            if (comp != null) {
                                                plansVm.createPlan(
                                                    companionId = comp.id,
                                                    companionIsGroup = comp.isGroup,
                                                    title = title,
                                                    placeNames = SampleData.places
                                                        .filter { selectedPlaceIDs.contains(it.id) }
                                                        .map { it.name },
                                                    voteUntilMs = deadline.voteUntilMillis(),
                                                ) { ok -> if (ok) showSuccess = true }
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp).navigationBarsPadding()
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Flow header ───────────────────────────────────────────────────────────

@Composable
private fun FlowHeader(step: Int, totalSteps: Int, onBack: () -> Unit) {
    val c = hh
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp).statusBarsPadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TextButton(onClick = onBack) {
            Text(if (step == 0) "Cancel" else "← Back", style = HHType.bodySm, fontWeight = FontWeight.SemiBold, color = c.inkMute)
        }

        // progress pills
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            (0 until totalSteps).forEach { i ->
                val active = i <= step
                val isCurrent = i == step
                val w by animateDpAsState(if (isCurrent) 18.dp else 5.dp, HHMotion.subtleDp, label = "dot$i")
                Box(
                    modifier = Modifier
                        .width(w).height(5.dp)
                        .clip(CircleShape)
                        .background(if (active) c.accent else c.strokeHi)
                )
            }
        }

        Text(
            "${step + 1} of $totalSteps",
            style = HHType.monoXs,
            color = c.inkDim,
            modifier = Modifier.width(48.dp),
        )
    }
}

// ── Stage header ──────────────────────────────────────────────────────────

@Composable
private fun StageHeader(kicker: String, heading: String, sub: String? = null) {
    val c = hh
    Column(modifier = Modifier.padding(horizontal = 18.dp).padding(bottom = 22.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        MonoLabel(kicker)
        Text(heading, style = HHType.display(30), color = c.ink, maxLines = 2)
        sub?.let { Text(it, style = HHType.bodySm, color = c.inkMute) }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MARK: Stages
// ═══════════════════════════════════════════════════════════════════════════

// Stage 0 — Title
@Composable
private fun Stage0Title(title: String, desc: String, onTitleChange: (String) -> Unit, onDescChange: (String) -> Unit) {
    val c = hh
    Column(modifier = Modifier.padding(top = 8.dp)) {
        StageHeader("Step 1", "What's the plan?", "A short title your friends will see.")
        Column(modifier = Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                placeholder = { Text("e.g. Cozy coffee", style = HHType.bodyMd, color = c.inkDim) },
                textStyle = HHType.bodyMd.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = c.ink),
                shape = RoundedCornerShape(HHRadius.lg),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = c.accent, unfocusedBorderColor = c.stroke,
                    focusedContainerColor = c.surface, unfocusedContainerColor = c.surface,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = desc,
                onValueChange = onDescChange,
                placeholder = { Text("Optional: add a vibe-check note", style = HHType.bodySm, color = c.inkDim) },
                textStyle = HHType.bodySm.copy(color = c.ink),
                shape = RoundedCornerShape(HHRadius.lg),
                minLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = c.accent, unfocusedBorderColor = c.stroke,
                    focusedContainerColor = c.surface, unfocusedContainerColor = c.surface,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// Stage 1 — Companion (real friends + groups from app state)
@Composable
private fun Stage1Companion(
    companion: CompanionSelection?,
    friends: List<HHUser>,
    groups: List<ChatDto>,
    onSelect: (CompanionSelection) -> Unit,
) {
    val c = hh
    Column(modifier = Modifier.padding(top = 8.dp)) {
        StageHeader("Step 2", "Who's this with?", "Pick a friend or a group.")

        Column(modifier = Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            if (groups.isEmpty() && friends.isEmpty()) {
                Text(
                    "No friends or groups yet — add friends from the Circles tab first.",
                    style = HHType.bodySm,
                    color = c.inkMute,
                )
            }

            if (groups.isNotEmpty()) {
                MonoLabel("Groups"); Spacer(Modifier.height(10.dp))
                groups.forEach { group ->
                    val sel = companion?.id == group.id
                    val emoji = group.emoji ?: "👥"
                    val name = group.name ?: "Group"
                    Surface(
                        onClick = { onSelect(CompanionSelection(group.id, name, emoji, group.members.size, true)) },
                        shape = RoundedCornerShape(HHRadius.lg),
                        color = if (sel) c.accentSoft else c.surface,
                        border = BorderStroke(if (sel) 1.5.dp else 1.dp, if (sel) c.accent else c.stroke),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    ) {
                        Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(HHRadius.md)).background(c.bgElev)) { Text(emoji, fontSize = 20.sp) }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(name, style = HHType.bodyMd, fontWeight = FontWeight.Bold, color = c.ink)
                                Text("${group.members.size} members", style = HHType.caption, color = c.inkMute)
                            }
                            if (sel) Icon(Icons.Default.Check, null, tint = c.accent, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }

            if (friends.isNotEmpty()) {
                MonoLabel("Friends"); Spacer(Modifier.height(10.dp))
                friends.forEach { u ->
                    val sel = companion?.id == u.id
                    Surface(
                        onClick = { onSelect(CompanionSelection(u.id, u.name, u.avatar, null, false)) },
                        shape = RoundedCornerShape(HHRadius.md),
                        color = if (sel) c.accentSoft else Color.Transparent,
                        border = BorderStroke(1.dp, if (sel) c.accent else Color.Transparent),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            AvatarView(user = u, size = 36.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(u.name, style = HHType.bodyMd, fontWeight = FontWeight.SemiBold, color = c.ink)
                                Text(u.status.label, style = HHType.caption, color = c.inkMute)
                            }
                            if (sel) Icon(Icons.Default.Check, null, tint = c.accent, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

// Stage 2 — Vibe
@Composable
private fun Stage2Vibe(vibe: HangVibe, onSelect: (HangVibe) -> Unit) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        StageHeader("Step 3", "Set the vibe.", "Pick one — we'll match places to it.")
        Column(modifier = Modifier.padding(horizontal = 18.dp)) {
            HangVibe.entries.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                    row.forEach { v ->
                        Box(modifier = Modifier.weight(1f)) {
                            VibeCard(vibe = v, selected = vibe == v, height = 110.dp, onClick = { onSelect(v) })
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

// Stage 3 — Places
@Composable
private fun Stage3Places(vibe: HangVibe, selectedIDs: Set<String>, loading: Boolean, onToggle: (String) -> Unit) {
    val c = hh
    Column(modifier = Modifier.padding(top = 8.dp)) {
        StageHeader("Step 4", "Pick places to vote on.", "Matched to ${vibe.label} · up to 3.")
        Column(modifier = Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // search hint
            Surface(shape = CircleShape, color = c.surface, border = BorderStroke(1.dp, c.stroke), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🔍", fontSize = 16.sp)
                    Text("Or search a specific spot…", style = HHType.bodySm, color = c.inkDim)
                }
            }

            MonoLabel(if (loading) "Finding your spots…" else "Suggested · ${vibe.label}")

            if (loading) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(3) {
                        Box(modifier = Modifier.fillMaxWidth().height(66.dp).clip(RoundedCornerShape(HHRadius.md)).background(c.bgElev).shimmer())
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SampleData.places.take(4).forEach { place ->
                        val sel = selectedIDs.contains(place.id)
                        Surface(
                            onClick = { onToggle(place.id) },
                            shape = RoundedCornerShape(HHRadius.lg),
                            color = if (sel) c.accentSoft else c.surface,
                            border = BorderStroke(if (sel) 1.5.dp else 1.dp, if (sel) c.accent else c.stroke),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(HHRadius.sm)).background(c.bgElev)) { Text(place.emoji, fontSize = 22.sp) }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(place.name, style = HHType.bodyMd, fontWeight = FontWeight.Bold, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("★ ${place.rating} · ${place.price} · ${place.match}% match", style = HHType.caption, color = c.inkMute)
                                }
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(22.dp).clip(CircleShape)
                                        .background(if (sel) c.accent else Color.Transparent)
                                        .border(1.5.dp, if (sel) c.accent else c.strokeHi, CircleShape)
                                ) {
                                    if (sel) Icon(Icons.Default.Check, null, tint = c.onAccent, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Stage 4 — Deadline
@Composable
private fun Stage4Deadline(deadline: DeadlinePreset, onSelect: (DeadlinePreset) -> Unit) {
    val c = hh
    Column(modifier = Modifier.padding(top = 8.dp)) {
        StageHeader("Step 5", "When does voting close?", "Fast deadlines get better turnout.")
        Column(modifier = Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DeadlinePreset.entries.forEach { preset ->
                val sel = deadline == preset
                Surface(
                    onClick = { onSelect(preset) },
                    shape = RoundedCornerShape(HHRadius.lg),
                    color = if (sel) c.accentSoft else c.surface,
                    border = BorderStroke(if (sel) 1.5.dp else 1.dp, if (sel) c.accent else c.stroke),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(preset.label, style = HHType.display(18), color = c.ink)
                            Text(preset.sub, style = HHType.caption, color = c.inkMute)
                        }
                        if (sel) Icon(Icons.Default.Check, null, tint = c.accent, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

// Stage 5 — Review
@Composable
private fun Stage5Review(title: String, vibe: HangVibe, companion: CompanionSelection?, selectedPlaceIDs: Set<String>, deadline: DeadlinePreset) {
    val c = hh
    val dark = c.isDark
    val selectedPlaces = SampleData.places.filter { selectedPlaceIDs.contains(it.id) }
    val companionDisplay = companion?.let { if (it.isGroup) "👥 ${it.name} (${it.count})" else "${it.emoji ?: ""} ${it.name}" } ?: "Baseball Buddies (4)"

    Column(modifier = Modifier.padding(top = 8.dp)) {
        StageHeader("Step 6", "Ready to post?", "Review and send — invites go out instantly.")
        Column(modifier = Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // review card
            HHCard {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    // title + vibe
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(HHRadius.md)).background(vibe.bg(dark))) { Text(vibe.emoji, fontSize = 26.sp) }
                        Column {
                            Text(title, style = HHType.display(22), color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${vibe.label} vibe", style = HHType.caption, color = c.inkMute)
                        }
                    }
                    HorizontalDivider(color = c.stroke)
                    // with
                    ReviewRow(label = "WITH", value = companionDisplay)
                    HorizontalDivider(color = c.stroke)
                    // places
                    Row(modifier = Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("OPTIONS", style = HHType.monoXs, color = c.inkDim, letterSpacing = 1.sp, modifier = Modifier.width(70.dp).padding(top = 3.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            (selectedPlaces.ifEmpty { SampleData.places.take(1) }).take(3).forEachIndexed { i, p ->
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("${i+1}", style = HHType.monoXs, color = c.inkMute)
                                    Text("${p.emoji} ${p.name}", style = HHType.bodySm, color = c.ink)
                                    Text("★ ${p.rating}", style = HHType.caption, color = c.inkMute)
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = c.stroke)
                    ReviewRow(label = "CLOSES", value = deadline.label)
                }
            }

            // channel hint
            Surface(shape = RoundedCornerShape(HHRadius.md), color = c.bgElev) {
                Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("💬", fontSize = 18.sp)
                    Text("Posts to #${companion?.name ?: "Baseball Buddies"}", style = HHType.caption, color = c.inkMute)
                }
            }
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    val c = hh
    Row(
        modifier = Modifier.padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = HHType.monoXs, color = c.inkDim, letterSpacing = 1.sp, modifier = Modifier.width(70.dp))
        Text(value, style = HHType.bodySm, fontWeight = FontWeight.SemiBold, color = c.ink)
    }
}

// ─── Success ──────────────────────────────────────────────────────────────

@Composable
private fun SuccessView(title: String, companion: CompanionSelection?, vibe: HangVibe) {
    val c = hh
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(50); revealed = true }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // confetti + check circle
            Box(modifier = Modifier.size(160.dp), contentAlignment = Alignment.Center) {
                // confetti dots
                (0 until 12).forEach { i ->
                    val angle = (i.toDouble() / 12) * Math.PI * 2
                    val radius = 70.0 + (i % 3) * 12
                    val colors = listOf(c.accent, c.statusFree, Color(0xFFE07A14), vibe.ink(c.isDark))
                    val scale by animateFloatAsState(if (revealed) 1f else 0f, spring<Float>(dampingRatio = 0.72f, stiffness = 200f).delay(i * 30), label = "confetti$i")
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .offset(
                                x = (cos(angle) * radius).dp - 4.dp,
                                y = (sin(angle) * radius).dp - 4.dp
                            )
                            .scale(scale)
                            .rotate((i * 30).toFloat())
                            .clip(RoundedCornerShape(2.dp))
                            .background(colors[i % colors.size])
                    )
                }

                // green circle
                val circleScale by animateFloatAsState(if (revealed) 1f else 0.3f, spring<Float>(dampingRatio = 0.72f, stiffness = 260f).delay(100), label = "circleScale")
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(110.dp).scale(circleScale).clip(CircleShape).background(c.statusFree.copy(alpha = 0.15f))
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(78.dp).clip(CircleShape).background(c.statusFree)) {
                        Text("✓", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Plan created!", style = HHType.display(30), color = c.ink)
                Text(
                    text = "$title posted to\n${companion?.name ?: "Baseball Buddies"}",
                    style = HHType.bodySm,
                    color = c.inkMute,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }

            Surface(shape = CircleShape, color = c.bgElev) {
                Text("💬 jumping to chat…", style = HHType.mono, color = c.accentInk, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
            }
        }
    }
}

// HHButton overload with Modifier
@Composable
private fun HHButton(
    label: String,
    style: HHButtonStyle,
    fullWidth: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = hh
    val bg  = when (style) { HHButtonStyle.PRIMARY -> c.accent; else -> c.surface }
    val fg  = when (style) { HHButtonStyle.PRIMARY -> c.onAccent; else -> c.ink }

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = bg,
        shadowElevation = if (style == HHButtonStyle.PRIMARY) 4.dp else 0.dp,
        modifier = modifier.then(if (fullWidth) Modifier.fillMaxWidth() else Modifier).height(54.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = HHType.bodyMd, fontWeight = FontWeight.SemiBold, color = fg, fontSize = 17.sp)
        }
    }
}

private fun <T> AnimationSpec<T>.delay(millis: Int): AnimationSpec<T> =
    if (millis == 0) this else object : AnimationSpec<T> by this {
        override fun <V : AnimationVector> vectorize(converter: TwoWayConverter<T, V>) =
            this@delay.vectorize(converter).run {
                object : VectorizedAnimationSpec<V> by this {
                    override val isInfinite get() = this@run.isInfinite
                    override fun getDurationNanos(initialValue: V, targetValue: V, initialVelocity: V): Long =
                        this@run.getDurationNanos(initialValue, targetValue, initialVelocity) + millis * 1_000_000L
                    override fun getValueFromNanos(playTimeNanos: Long, initialValue: V, targetValue: V, initialVelocity: V) =
                        this@run.getValueFromNanos((playTimeNanos - millis * 1_000_000L).coerceAtLeast(0L), initialValue, targetValue, initialVelocity)
                    override fun getVelocityFromNanos(playTimeNanos: Long, initialValue: V, targetValue: V, initialVelocity: V) =
                        this@run.getVelocityFromNanos((playTimeNanos - millis * 1_000_000L).coerceAtLeast(0L), initialValue, targetValue, initialVelocity)
                }
            }
    }
