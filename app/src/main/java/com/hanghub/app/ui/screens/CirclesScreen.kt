package com.hanghub.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.hanghub.app.ToastData
import com.hanghub.app.data.*
import com.hanghub.app.ui.chrome.LocalAppChrome
import com.hanghub.app.ui.components.*
import com.hanghub.app.ui.theme.*

// ═══════════════════════════════════════════════════════════════════════════
// MARK: CirclesScreen
// ═══════════════════════════════════════════════════════════════════════════

enum class CirclesTab { FRIENDS, CHATS, GROUPS }

@Composable
fun CirclesScreen(showToast: (ToastData) -> Unit) {
    val c = hh
    var activeTab by remember { mutableStateOf(CirclesTab.FRIENDS) }
    var selectedChat by remember { mutableStateOf<HHChat?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(c.bg)) {
        // ── Header ─────────────────────────────────────────────────────
        Column(modifier = Modifier.padding(top = 16.dp, start = 18.dp, end = 18.dp, bottom = 12.dp)) {
            MonoLabel("Circles")
            Text("Your people", style = HHType.display(30), color = c.ink)
        }

        // ── Segmented control ───────────────────────────────────────────
        Surface(
            shape = CircleShape,
            color = c.bgElev,
            modifier = Modifier.padding(horizontal = 18.dp).fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                CirclesTab.entries.forEach { tab ->
                    val active = tab == activeTab
                    Surface(
                        onClick = { activeTab = tab },
                        shape = CircleShape,
                        color = if (active) c.surface else Color.Transparent,
                        shadowElevation = if (active) 2.dp else 0.dp,
                        modifier = Modifier.weight(1f).height(36.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                tab.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = HHType.bodySm,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                color = if (active) c.ink else c.inkMute,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // ── Tab content ─────────────────────────────────────────────────
        when (activeTab) {
            CirclesTab.FRIENDS -> FriendsTab()
            CirclesTab.CHATS   -> ChatsTab(onOpenChat = { selectedChat = it })
            CirclesTab.GROUPS  -> GroupsTab()
        }
    }

    selectedChat?.let { chat ->
        ChatScreen(chat = chat, onDismiss = { selectedChat = null })
    }
}

// ── Friends tab ───────────────────────────────────────────────────────────

@Composable
private fun FriendsTab() {
    val c = hh
    val freeUsers    = SampleData.users.filter { it.status == UserStatus.FREE    && it.id != "me" }
    val notFreeUsers = SampleData.users.filter { it.status != UserStatus.FREE    && it.id != "me" }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item { MonoLabel("Free now · ${freeUsers.size}"); Spacer(Modifier.height(10.dp)) }
        items(freeUsers) { u -> FriendRow(user = u); Spacer(Modifier.height(2.dp)) }
        item { Spacer(Modifier.height(18.dp)); MonoLabel("Busy or away"); Spacer(Modifier.height(10.dp)) }
        items(notFreeUsers) { u -> FriendRow(user = u); Spacer(Modifier.height(2.dp)) }
        item { Spacer(Modifier.height(110.dp)) }
    }
}

@Composable
private fun FriendRow(user: HHUser) {
    val c = hh
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
    ) {
        AvatarView(user = user, size = 40.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(user.name, style = HHType.bodyMd, fontWeight = FontWeight.SemiBold, color = c.ink)
                AuraBadge(user.aura)
            }
            Text(
                when (user.status) {
                    UserStatus.FREE    -> "${user.distance?.let { "${String.format("%.1f", it)}mi away · " } ?: ""}free"
                    UserStatus.BUSY    -> "busy now"
                    UserStatus.OFFLINE -> "last seen 2h ago"
                },
                style = HHType.caption,
                color = c.inkMute,
            )
        }
        if (user.status == UserStatus.FREE) {
            Surface(shape = CircleShape, color = c.accentSoft) {
                Text("Invite", style = HHType.caption, fontWeight = FontWeight.Bold, color = c.accentInk, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            }
        }
    }
}

// ── Chats tab ─────────────────────────────────────────────────────────────

@Composable
private fun ChatsTab(onOpenChat: (HHChat) -> Unit) {
    val c = hh
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(SampleData.chats) { chat ->
            val user = SampleData.user(chat.userID) ?: return@items
            Surface(
                onClick = { onOpenChat(chat) },
                shape = RoundedCornerShape(HHRadius.lg),
                color = if (chat.unread > 0) c.surface else Color.Transparent,
                border = if (chat.unread > 0) BorderStroke(1.dp, c.stroke) else null,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AvatarView(user = user, size = 44.dp)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(user.name, style = HHType.bodyMd, fontWeight = FontWeight.SemiBold, color = c.ink)
                            Text(chat.time, style = HHType.caption, color = c.inkDim)
                        }
                        if (chat.isTyping) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                TypingIndicator()
                                Text("typing…", style = HHType.bodySm, color = c.accent)
                            }
                        } else {
                            Text(chat.lastMessage, style = HHType.bodySm, color = c.inkMute, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    if (chat.unread > 0) {
                        Surface(shape = CircleShape, color = c.accent) {
                            Text("${chat.unread}", style = HHType.caption, fontWeight = FontWeight.Bold, color = c.onAccent, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(110.dp)) }
    }
}

// ── Groups tab ────────────────────────────────────────────────────────────

private val groups = listOf(
    Triple("The Brunch Club", "🥞", 6 to true),
    Triple("Hiking Crew",     "🗻", 8 to false),
    Triple("Movie Buddies",   "🎬", 4 to false),
)

@Composable
private fun GroupsTab() {
    val c = hh
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(groups) { (name, emoji, meta) ->
            HHCard {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(HHRadius.md)).background(c.bgElev)
                    ) { Text(emoji, fontSize = 22.sp) }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(name, style = HHType.bodyMd, fontWeight = FontWeight.SemiBold, color = c.ink)
                        Text("${meta.first} members", style = HHType.caption, color = c.inkMute)
                    }
                    if (meta.second) LiveDot(color = c.accent, label = "planning now")
                }
            }
        }
        item { Spacer(Modifier.height(110.dp)) }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MARK: ChatScreen
// ═══════════════════════════════════════════════════════════════════════════

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ChatScreen(chat: HHChat, onDismiss: () -> Unit) {
    val c = hh
    val chrome = LocalAppChrome.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val user = SampleData.user(chat.userID) ?: return
    var planCardState by remember { mutableStateOf(PlanCardState.VOTING) }
    val states = PlanCardState.entries

    // Hide the floating tab bar while this sheet is shown
    DisposableEffect(Unit) {
        val pop = chrome.pushTabBarHidden()
        onDispose { pop() }
    }

    LaunchedEffect(Unit) {
        sheetState.expand()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = c.bg,
        modifier = Modifier.fillMaxSize(),
        dragHandle = null,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Chat header ─────────────────────────────────────────────
            Surface(color = c.bg, border = BorderStroke(width = 0.dp, color = Color.Transparent)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(onClick = onDismiss, shape = CircleShape, color = Color.Transparent) {
                        Text("←", fontSize = 20.sp, color = c.ink, modifier = Modifier.padding(4.dp))
                    }
                    AvatarView(user = user, size = 36.dp)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(user.name, style = HHType.bodyMd, fontWeight = FontWeight.Bold, color = c.ink)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            TypingIndicator()
                            Text("typing…", style = HHType.caption, color = c.accent)
                        }
                    }
                    Surface(shape = CircleShape, color = c.surface, border = BorderStroke(1.dp, c.stroke)) {
                        Text("+ Plan", style = HHType.caption, fontWeight = FontWeight.Bold, color = c.ink, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                    }
                }
                HorizontalDivider(color = c.stroke)
            }

            // ── Messages ─────────────────────────────────────────────────
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("TODAY", style = HHType.monoXs, color = c.inkDim, modifier = Modifier.padding(vertical = 10.dp))
                    }
                }

                items(SampleData.chatThread.take(3)) { msg -> BubbleView(message = msg) }

                item {
                    // Plan message card
                    PlanMessageCard(state = planCardState, onCycleState = {
                        planCardState = states[(states.indexOf(planCardState) + 1) % states.size]
                    })
                    // cycle button
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 6.dp), contentAlignment = Alignment.Center) {
                        Surface(
                            onClick = { planCardState = states[(states.indexOf(planCardState) + 1) % states.size] },
                            shape = CircleShape,
                            color = c.bgElev,
                            border = BorderStroke(1.dp, c.stroke),
                        ) {
                            Text(
                                "tap to cycle · ${planCardState.name.lowercase()}",
                                style = HHType.monoXs,
                                color = c.inkMute,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                items(SampleData.chatThread.drop(3)) { msg -> BubbleView(message = msg) }

                item {
                    // typing bubble
                    Row {
                        Surface(shape = RoundedCornerShape(20.dp), color = c.surface, border = BorderStroke(1.dp, c.stroke)) {
                            Box(modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp)) { TypingIndicator() }
                        }
                        Spacer(Modifier.weight(1f))
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            // ── Composer ──────────────────────────────────────────────────
            HorizontalDivider(color = c.stroke)
            Row(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = c.bgElev,
                    border = BorderStroke(1.dp, c.stroke),
                    modifier = Modifier.weight(1f).height(48.dp),
                ) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                        Text("Message ${user.name.split(" ").first()}…", style = HHType.bodySm, color = c.inkDim)
                    }
                }
                Surface(shape = CircleShape, color = c.accent, modifier = Modifier.size(42.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text("→", fontSize = 16.sp, color = c.onAccent, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun BubbleView(message: ChatMessage) {
    val c = hh
    val isMine = message.from == "me"
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start) {
        if (isMine) Spacer(Modifier.weight(1f).widthIn(min = 60.dp))
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isMine) c.accent else c.surface,
            border = if (isMine) null else BorderStroke(1.dp, c.stroke),
        ) {
            Text(
                message.text,
                style = HHType.body,
                color = if (isMine) c.onAccent else c.ink,
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp)
            )
        }
        if (!isMine) Spacer(Modifier.weight(1f).widthIn(min = 60.dp))
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MARK: PlanMessageCard — 4 states in the chat thread
// ═══════════════════════════════════════════════════════════════════════════

enum class PlanCardState { SUGGESTION, VOTING, RSVP, FINALIZED }

@Composable
fun PlanMessageCard(state: PlanCardState, onCycleState: () -> Unit) {
    val c = hh
    val dark = c.isDark

    val headerBg: Color
    val headerInk: Color
    val headerLabel: String
    when (state) {
        PlanCardState.SUGGESTION -> { headerBg = Color(0xFFFBE8CB); headerInk = Color(0xFF8A5510); headerLabel = "New hangout" }
        PlanCardState.VOTING     -> { headerBg = c.accentSoft;       headerInk = c.accentInk;       headerLabel = "Voting · 2h 15m left" }
        PlanCardState.RSVP       -> { headerBg = c.accentSoft;       headerInk = c.accentInk;       headerLabel = "Locked in · RSVP" }
        PlanCardState.FINALIZED  -> { headerBg = Color(0xFFD6EEDF);  headerInk = Color(0xFF1C5A3A); headerLabel = "Confirmed · Today" }
    }

    Surface(
        shape = RoundedCornerShape(HHRadius.lg),
        color = c.surface,
        border = BorderStroke(1.dp, c.stroke),
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = HHRadius.lg, topEnd = HHRadius.lg))
                    .background(headerBg)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("⚾", fontSize = 18.sp)
                    Column {
                        Text(headerLabel.uppercase(), style = HHType.monoXs, color = headerInk.copy(alpha = 0.75f))
                        Text("Yankees Game", style = HHType.display(17), color = headerInk)
                    }
                }
                if (state == PlanCardState.VOTING) {
                    Surface(shape = CircleShape, color = c.surface) {
                        Text("2:15", style = HHType.mono, fontWeight = FontWeight.Bold, color = headerInk, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
                if (state == PlanCardState.FINALIZED) {
                    Text("✓", fontSize = 20.sp, color = c.statusFree)
                }
            }

            // Body
            when (state) {
                PlanCardState.SUGGESTION -> SuggestionBody(headerInk, onCycleState)
                PlanCardState.VOTING     -> VotingBody(onCycleState)
                PlanCardState.RSVP       -> RsvpBody(onCycleState)
                PlanCardState.FINALIZED  -> FinalizedBody()
            }
        }
    }
}

@Composable
private fun SuggestionBody(accentInk: Color, onJoin: () -> Unit) {
    val c = hh
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HHCard(padding = PaddingValues(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(HHRadius.sm)).background(c.bgElev)) { Text("🏟️", fontSize = 22.sp) }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Yankee Stadium", style = HHType.bodySm, fontWeight = FontWeight.Bold, color = c.ink)
                    Text("★ 4.5 · $$$$ · 2.1 mi", style = HHType.caption, color = c.inkMute)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Voting opens 2:00 PM", style = HHType.caption, color = c.inkMute)
            Surface(onClick = onJoin, shape = CircleShape, color = accentInk) {
                Text("Join", style = HHType.caption, fontWeight = FontWeight.Bold, color = Color(0xFFFBE8CB), modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
            }
        }
    }
}

@Composable
private fun VotingBody(onVote: () -> Unit) {
    val c = hh
    val options = listOf(Triple("🏟️","Yankee Stadium", 70), Triple("🍕","Lombardi's Pizza",30), Triple("🍔","Shake Shack",0))
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEachIndexed { i, (emoji, name, pct) ->
            val animPct by animateFloatAsState(pct / 100f, spring<Float>(dampingRatio = 0.72f).delay(i * 80), label = "pct$i")
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(HHRadius.md)).background(c.bgElev).border(1.dp, c.stroke, RoundedCornerShape(HHRadius.md))
            ) {
                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(animPct).background(c.accentSoft.copy(alpha = 0.55f)))
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(emoji, fontSize = 16.sp)
                    Text(name, style = HHType.bodySm, fontWeight = FontWeight.SemiBold, color = c.ink, modifier = Modifier.weight(1f))
                    Text("$pct%", style = HHType.mono, fontWeight = FontWeight.Bold, color = c.ink)
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            LiveDot(color = c.accent, label = "4 / 4 VOTING")
            Surface(onClick = onVote, shape = CircleShape, color = c.accent) {
                Text("Vote", style = HHType.caption, fontWeight = FontWeight.Bold, color = c.onAccent, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
            }
        }
    }
}

@Composable
private fun RsvpBody(onRsvp: () -> Unit) {
    val c = hh
    var choice by remember { mutableStateOf<String?>(null) }
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HHCard(padding = PaddingValues(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(HHRadius.sm)).background(c.bgElev)) { Text("🏟️", fontSize = 26.sp) }
                Column { Text("Yankee Stadium", style = HHType.bodySm, fontWeight = FontWeight.Bold, color = c.ink); Text("Sat Apr 20 · 6:00 PM", style = HHType.caption, color = c.inkMute) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("✓ 3 confirmed", style = HHType.caption, fontWeight = FontWeight.Bold, color = c.statusFree)
            Text("·", style = HHType.caption, color = c.inkMute)
            Text("? 1 maybe", style = HHType.caption, fontWeight = FontWeight.Bold, color = c.statusBusy)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(Triple("yes","I'm in",c.statusFree), Triple("maybe","Maybe",c.statusBusy), Triple("no","Can't",Color(0xFFE2442F))).forEach { (k, l, col) ->
                Surface(
                    onClick = { choice = k; onRsvp() },
                    shape = CircleShape,
                    color = if (choice == k) col else c.surface,
                    border = BorderStroke(1.5.dp, if (choice == k) Color.Transparent else c.stroke),
                    modifier = if (k == "yes") Modifier.weight(2f).height(36.dp) else Modifier.width(70.dp).height(36.dp),
                ) { Box(contentAlignment = Alignment.Center) { Text(l, style = HHType.caption, fontWeight = FontWeight.Bold, color = if (choice == k) Color.White else c.ink) } }
            }
        }
    }
}

@Composable
private fun FinalizedBody() {
    val c = hh
    val participants = SampleData.users.take(4)
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(HHRadius.md)).background(Color(0xFFD6EEDF)).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🏟️", fontSize = 28.sp)
            Column { Text("Yankee Stadium", style = HHType.bodySm, fontWeight = FontWeight.Bold, color = Color(0xFF1C5A3A)); Text("Bronx, NY · Sat Apr 20 · 6:00 PM", style = HHType.caption, color = Color(0xFF1C5A3A).copy(alpha = 0.75f)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            AvatarStack(participants.map { it.id }, size = 28.dp)
            Text("${participants.size} going", style = HHType.caption, fontWeight = FontWeight.SemiBold, color = c.ink)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Cost split" to "You owe $0", "Ticket" to "#YKEES2847").forEach { (label, value) ->
                Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(HHRadius.sm)).background(c.bgElev).border(1.dp, c.stroke, RoundedCornerShape(HHRadius.sm)).padding(horizontal = 10.dp, vertical = 8.dp)) {
                    MonoLabel(label); Spacer(Modifier.height(2.dp)); Text(value, style = HHType.bodySm, fontWeight = FontWeight.Bold, color = c.ink)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(shape = CircleShape, color = c.ink, modifier = Modifier.weight(1f).height(38.dp)) { Box(contentAlignment = Alignment.Center) { Text("🗺 Open in Maps", style = HHType.caption, fontWeight = FontWeight.Bold, color = c.onAccent) } }
            Surface(shape = CircleShape, color = c.surface, border = BorderStroke(1.dp, c.stroke), modifier = Modifier.weight(1f).height(38.dp)) { Box(contentAlignment = Alignment.Center) { Text("Details", style = HHType.caption, fontWeight = FontWeight.Bold, color = c.ink) } }
        }
    }
}

// AnimationSpec delay extension
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
