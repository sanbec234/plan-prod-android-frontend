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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hanghub.app.ToastData
import com.hanghub.app.core.appContainer
import com.hanghub.app.core.viewModelFactory
import com.hanghub.app.data.*
import com.hanghub.app.data.dto.ChatDto
import com.hanghub.app.data.dto.FriendRequestDto
import com.hanghub.app.data.dto.SearchUserDto
import com.hanghub.app.ui.chrome.LocalAppChrome
import com.hanghub.app.ui.components.*
import com.hanghub.app.ui.state.AppStateViewModel
import com.hanghub.app.ui.state.LocalAppState
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
    var showCreateGroup by remember { mutableStateOf(false) }
    var groupDetails by remember { mutableStateOf<ChatDto?>(null) }

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
            CirclesTab.FRIENDS -> FriendsTab(showToast = showToast)
            CirclesTab.CHATS   -> ChatsTab(onOpenChat = { selectedChat = it })
            CirclesTab.GROUPS  -> GroupsTab(
                onOpenChat = { selectedChat = it },
                onCreateGroup = { showCreateGroup = true },
                onOpenDetails = { groupDetails = it },
            )
        }
    }

    selectedChat?.let { chat ->
        ChatScreen(chat = chat, onDismiss = { selectedChat = null })
    }

    if (showCreateGroup) {
        CreateGroupSheet(
            onDismiss = { showCreateGroup = false },
            onCreated = {
                showCreateGroup = false
                showToast(ToastData("Group created!"))
            },
        )
    }

    groupDetails?.let { group ->
        GroupDetailsSheet(
            group = group,
            onDismiss = { groupDetails = null },
            onLeft = {
                groupDetails = null
                showToast(ToastData("Left ${group.name ?: "group"}"))
            },
        )
    }
}

// ── Friends tab ───────────────────────────────────────────────────────────

@Composable
private fun FriendsTab(showToast: (ToastData) -> Unit) {
    val c = hh
    val appState = LocalAppState.current
    val container = appContainer()
    val vm: FriendsViewModel = viewModel(
        factory = viewModelFactory { FriendsViewModel(appState, container.friendsRepository) }
    )

    LaunchedEffect(vm.actionError) {
        vm.actionError?.let { showToast(ToastData(it)); vm.clearError() }
    }

    val friends = appState.friends
    val searching = vm.searchQuery.isNotBlank()

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // ── Search field ────────────────────────────────────────────────
        item {
            OutlinedTextField(
                value = vm.searchQuery,
                onValueChange = vm::onSearchQueryChange,
                placeholder = { Text("Search people by name or @handle", style = HHType.bodySm, color = c.inkDim) },
                singleLine = true,
                shape = CircleShape,
                textStyle = HHType.bodySm.copy(color = c.ink),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = c.accent, unfocusedBorderColor = c.stroke,
                    focusedContainerColor = c.surface, unfocusedContainerColor = c.surface,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
        }

        if (searching) {
            // ── Search results ──────────────────────────────────────────
            if (vm.isSearching) {
                item { CenteredSpinner() }
            } else if (vm.searchResults.isEmpty()) {
                item { Text("No people found.", style = HHType.bodySm, color = c.inkMute) }
            } else {
                items(vm.searchResults, key = { it.id }) { result ->
                    SearchResultRow(
                        result = result,
                        alreadySent = vm.sentTo.contains(result.id),
                        onAdd = { vm.sendRequest(result.id) },
                    )
                    Spacer(Modifier.height(2.dp))
                }
            }
        } else {
            // ── Incoming requests ───────────────────────────────────────
            if (vm.requests.isNotEmpty()) {
                item {
                    MonoLabel("Friend requests · ${vm.requests.size}")
                    Spacer(Modifier.height(10.dp))
                }
                items(vm.requests, key = { it.id }) { req ->
                    FriendRequestRow(
                        request = req,
                        onAccept = { vm.accept(req.id) },
                        onDecline = { vm.decline(req.id) },
                    )
                    Spacer(Modifier.height(6.dp))
                }
                item { Spacer(Modifier.height(14.dp)) }
            }

            // ── Friends list ────────────────────────────────────────────
            val freeUsers = friends.filter { it.status == UserStatus.FREE }
            val notFreeUsers = friends.filter { it.status != UserStatus.FREE }

            when {
                appState.isLoadingCircles && friends.isEmpty() -> item { CenteredSpinner() }
                friends.isEmpty() -> item {
                    Text(
                        "No friends yet — search above to send your first request.",
                        style = HHType.bodySm, color = c.inkMute,
                    )
                }
                else -> {
                    item { MonoLabel("Free now · ${freeUsers.size}"); Spacer(Modifier.height(10.dp)) }
                    items(freeUsers, key = { it.id }) { u ->
                        FriendRow(user = u, onRemove = { vm.removeFriend(u.id); showToast(ToastData("Removed ${u.name}")) })
                        Spacer(Modifier.height(2.dp))
                    }
                    if (notFreeUsers.isNotEmpty()) {
                        item { Spacer(Modifier.height(18.dp)); MonoLabel("Busy or away"); Spacer(Modifier.height(10.dp)) }
                        items(notFreeUsers, key = { it.id }) { u ->
                            FriendRow(user = u, onRemove = { vm.removeFriend(u.id); showToast(ToastData("Removed ${u.name}")) })
                            Spacer(Modifier.height(2.dp))
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(110.dp)) }
    }
}

@Composable
private fun CenteredSpinner() {
    val c = hh
    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = c.accent)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FriendRow(user: HHUser, onRemove: () -> Unit) {
    val c = hh
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onRemove)
            .padding(vertical = 10.dp),
    ) {
        AvatarView(user = user, size = 40.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(user.name, style = HHType.bodyMd, fontWeight = FontWeight.SemiBold, color = c.ink)
                AuraBadge(user.aura)
            }
            Text(
                user.username?.let { "@$it" } ?: user.status.label,
                style = HHType.caption,
                color = c.inkMute,
            )
        }
    }
}

@Composable
private fun FriendRequestRow(
    request: FriendRequestDto,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    val c = hh
    Surface(
        shape = RoundedCornerShape(HHRadius.lg),
        color = c.surface,
        border = BorderStroke(1.dp, c.stroke),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(40.dp).clip(CircleShape).background(c.bgElev),
            ) { Text(request.fromAvatar.ifBlank { "👤" }, fontSize = 20.sp) }
            Column(modifier = Modifier.weight(1f)) {
                Text(request.fromName, style = HHType.bodyMd, fontWeight = FontWeight.SemiBold, color = c.ink)
                Text("wants to connect", style = HHType.caption, color = c.inkMute)
            }
            Surface(onClick = onDecline, shape = CircleShape, color = c.bgElev) {
                Text("✕", fontSize = 14.sp, color = c.inkMute, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
            }
            Surface(onClick = onAccept, shape = CircleShape, color = c.accent) {
                Text("Accept", style = HHType.caption, fontWeight = FontWeight.Bold, color = c.onAccent, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    result: SearchUserDto,
    alreadySent: Boolean,
    onAdd: () -> Unit,
) {
    val c = hh
    val status = result.friendshipStatus
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(40.dp).clip(CircleShape).background(c.bgElev),
        ) { Text(result.avatar.ifBlank { "👤" }, fontSize = 20.sp) }
        Column(modifier = Modifier.weight(1f)) {
            Text(result.name, style = HHType.bodyMd, fontWeight = FontWeight.SemiBold, color = c.ink)
            Text("@${result.username}", style = HHType.caption, color = c.inkMute)
        }
        when {
            status == "accepted" ->
                Text("Friends", style = HHType.caption, color = c.inkMute)
            status == "pending" || alreadySent ->
                Text("Requested", style = HHType.caption, color = c.accentInk)
            else -> Surface(onClick = onAdd, shape = CircleShape, color = c.accentSoft) {
                Text("Add", style = HHType.caption, fontWeight = FontWeight.Bold, color = c.accentInk, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
            }
        }
    }
}

// ── Chats tab ─────────────────────────────────────────────────────────────

@Composable
private fun ChatsTab(onOpenChat: (HHChat) -> Unit) {
    val c = hh
    val appState = LocalAppState.current
    val chats = appState.chats
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (appState.isLoadingCircles && chats.isEmpty()) {
            item { CenteredSpinner() }
        }
        if (!appState.isLoadingCircles && chats.isEmpty()) {
            item {
                Text(
                    "No conversations yet — create a plan with a friend to start one.",
                    style = HHType.bodySm, color = c.inkMute,
                )
            }
        }
        items(chats, key = { it.id }) { chat ->
            val user = HHUser(
                id = chat.userID,
                name = chat.partnerName.ifBlank { "Chat" },
                avatar = chat.partnerAvatar,
                status = UserStatus.OFFLINE,
                aura = 0,
            )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupsTab(
    onOpenChat: (HHChat) -> Unit,
    onCreateGroup: () -> Unit,
    onOpenDetails: (ChatDto) -> Unit,
) {
    val c = hh
    val appState = LocalAppState.current
    val groups = appState.groups
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Surface(
                onClick = onCreateGroup,
                shape = RoundedCornerShape(HHRadius.lg),
                color = c.accentSoft,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = c.accentInk)
                    Text("New group", style = HHType.bodyMd, fontWeight = FontWeight.Bold, color = c.accentInk)
                }
            }
        }

        if (appState.isLoadingCircles && groups.isEmpty()) {
            item { CenteredSpinner() }
        }
        if (!appState.isLoadingCircles && groups.isEmpty()) {
            item {
                Text("No groups yet — create one to plan together.", style = HHType.bodySm, color = c.inkMute)
            }
        }

        items(groups, key = { it.id }) { group ->
            HHCard(
                modifier = Modifier.combinedClickable(
                    onClick = { onOpenChat(group.toHHChat(appState.currentUserId)) },
                    onLongClick = { onOpenDetails(group) },
                )
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(HHRadius.md)).background(c.bgElev),
                    ) { Text(group.emoji ?: "👥", fontSize = 22.sp) }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(group.name ?: "Group", style = HHType.bodyMd, fontWeight = FontWeight.SemiBold, color = c.ink)
                        Text("${group.members.size} members", style = HHType.caption, color = c.inkMute)
                    }
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
    val appState = LocalAppState.current
    val container = appContainer()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val vm: ChatViewModel = viewModel(
        key = "chat-${chat.id}",
        factory = viewModelFactory {
            ChatViewModel(
                roomId = chat.id,
                currentUserId = appState.currentUserId,
                chatRepository = container.chatRepository,
                webSocketManager = container.webSocketManager,
            )
        },
    )
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val partner = HHUser(
        id = chat.userID,
        name = chat.partnerName.ifBlank { "Chat" },
        avatar = chat.partnerAvatar,
        status = UserStatus.OFFLINE,
        aura = 0,
    )

    DisposableEffect(Unit) {
        val pop = chrome.pushTabBarHidden()
        onDispose { pop() }
    }
    LaunchedEffect(Unit) { sheetState.expand() }
    LaunchedEffect(vm.messages.size) {
        if (vm.messages.isNotEmpty()) listState.animateScrollToItem(vm.messages.size - 1)
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
            Surface(color = c.bg) {
                Column {
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
                        AvatarView(user = partner, size = 36.dp)
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(partner.name, style = HHType.bodyMd, fontWeight = FontWeight.Bold, color = c.ink)
                            if (vm.partnerTyping) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    TypingIndicator()
                                    Text("typing…", style = HHType.caption, color = c.accent)
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = c.stroke)
                }
            }

            // ── Messages ─────────────────────────────────────────────────
            Box(modifier = Modifier.weight(1f)) {
                when {
                    vm.isLoading -> CenteredSpinner()
                    vm.error != null && vm.messages.isEmpty() ->
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Text(vm.error ?: "", style = HHType.bodySm, color = c.inkMute)
                        }
                    vm.messages.isEmpty() ->
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Text("Say hi 👋", style = HHType.bodySm, color = c.inkMute)
                        }
                    else -> LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(vm.messages, key = { it.id.ifBlank { it.hashCode().toString() } }) { msg ->
                            BubbleView(message = msg)
                        }
                    }
                }
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
                OutlinedTextField(
                    value = vm.draft,
                    onValueChange = vm::onDraftChange,
                    placeholder = {
                        Text("Message ${partner.name.split(" ").first()}…", style = HHType.bodySm, color = c.inkDim)
                    },
                    singleLine = true,
                    shape = CircleShape,
                    textStyle = HHType.bodySm.copy(color = c.ink),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = c.accent, unfocusedBorderColor = c.stroke,
                        focusedContainerColor = c.bgElev, unfocusedContainerColor = c.bgElev,
                    ),
                    modifier = Modifier.weight(1f),
                )
                Surface(onClick = { vm.send() }, shape = CircleShape, color = c.accent, modifier = Modifier.size(48.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("→", fontSize = 16.sp, color = c.onAccent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun BubbleView(message: ChatMessage) {
    val c = hh

    // System notes render as a centered, muted line — not a chat bubble.
    val payload = message.payload
    if (payload is MessagePayload.SystemNote) {
        Box(Modifier.fillMaxWidth().padding(vertical = 2.dp), contentAlignment = Alignment.Center) {
            Text(payload.text, style = HHType.caption, color = c.inkMute)
        }
        return
    }

    val isMine = message.from == "me"
    val fg = if (isMine) c.onAccent else c.ink
    val subFg = if (isMine) c.onAccent.copy(alpha = 0.7f) else c.inkMute
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start) {
        if (isMine) Spacer(Modifier.weight(1f).widthIn(min = 60.dp))
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isMine) c.accent else c.surface,
            border = if (isMine) null else BorderStroke(1.dp, c.stroke),
        ) {
            Column(modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp)) {
                when (payload) {
                    is MessagePayload.LocationPin -> {
                        Text("📍 ${payload.placeName}", style = HHType.body, fontWeight = FontWeight.SemiBold, color = fg)
                        payload.address?.takeIf { it.isNotBlank() }?.let {
                            Text(it, style = HHType.caption, color = subFg)
                        }
                    }
                    is MessagePayload.PlanCard -> {
                        Text("📋 ${payload.title}", style = HHType.body, fontWeight = FontWeight.SemiBold, color = fg)
                        payload.venueName?.takeIf { it.isNotBlank() }?.let {
                            Text(it, style = HHType.caption, color = subFg)
                        }
                    }
                    is MessagePayload.Image ->
                        Text("📷 Photo", style = HHType.body, color = fg)
                    else ->
                        Text(message.text, style = HHType.body, color = fg)
                }
            }
        }
        if (!isMine) Spacer(Modifier.weight(1f).widthIn(min = 60.dp))
    }
}
