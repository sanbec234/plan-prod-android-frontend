package com.hanghub.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import com.hanghub.app.core.appContainer
import com.hanghub.app.core.viewModelFactory
import com.hanghub.app.data.dto.ChatDto
import com.hanghub.app.ui.components.AvatarView
import com.hanghub.app.ui.components.MonoLabel
import com.hanghub.app.data.HHUser
import com.hanghub.app.ui.state.LocalAppState
import com.hanghub.app.ui.theme.HHRadius
import com.hanghub.app.ui.theme.HHType
import com.hanghub.app.ui.theme.UserStatus
import com.hanghub.app.ui.theme.hh

private val GROUP_EMOJIS = listOf("🎉", "🥞", "🗻", "🎬", "⚾", "☕", "🍷", "🎮", "🏀", "🎨")

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CreateGroupSheet(onDismiss: () -> Unit, onCreated: () -> Unit) {
    val c = hh
    val appState = LocalAppState.current
    val container = appContainer()
    val vm: GroupsViewModel = viewModel(
        factory = viewModelFactory { GroupsViewModel(appState, container.chatRepository) }
    )
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf(GROUP_EMOJIS.first()) }
    var selected by remember { mutableStateOf(setOf<String>()) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = c.bg) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(18.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("New group", style = HHType.display(24), color = c.ink)

            EmojiPicker(selected = emoji, onSelect = { emoji = it })

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Group name", style = HHType.bodySm, color = c.inkDim) },
                singleLine = true,
                shape = RoundedCornerShape(HHRadius.lg),
                textStyle = HHType.bodyMd.copy(color = c.ink),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = c.accent, unfocusedBorderColor = c.stroke,
                    focusedContainerColor = c.surface, unfocusedContainerColor = c.surface,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            MonoLabel("Add friends · ${selected.size}")
            if (appState.friends.isEmpty()) {
                Text("No friends yet — add some from the Friends tab.", style = HHType.caption, color = c.inkMute)
            }
            appState.friends.forEach { friend ->
                MemberToggleRow(
                    user = friend,
                    checked = selected.contains(friend.id),
                    onToggle = {
                        selected = if (selected.contains(friend.id)) selected - friend.id
                        else selected + friend.id
                    },
                )
            }

            vm.actionError?.let { Text(it, style = HHType.caption, color = Color(0xFFE2442F)) }

            Surface(
                onClick = {
                    if (!vm.isWorking && selected.isNotEmpty()) {
                        vm.createGroup(name, emoji, selected.toList()) { ok -> if (ok) onCreated() }
                    }
                },
                shape = CircleShape,
                color = if (selected.isEmpty()) c.bgElev else c.accent,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        if (vm.isWorking) "Creating…" else "Create group",
                        style = HHType.bodyMd, fontWeight = FontWeight.Bold,
                        color = if (selected.isEmpty()) c.inkMute else c.onAccent,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun GroupDetailsSheet(group: ChatDto, onDismiss: () -> Unit, onLeft: () -> Unit) {
    val c = hh
    val appState = LocalAppState.current
    val container = appContainer()
    val vm: GroupsViewModel = viewModel(
        factory = viewModelFactory { GroupsViewModel(appState, container.chatRepository) }
    )
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf(group.name ?: "") }
    var emoji by remember { mutableStateOf(group.emoji ?: GROUP_EMOJIS.first()) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = c.bg) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(18.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Group details", style = HHType.display(24), color = c.ink)

            EmojiPicker(selected = emoji, onSelect = { emoji = it })

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Group name", style = HHType.bodySm, color = c.inkDim) },
                singleLine = true,
                shape = RoundedCornerShape(HHRadius.lg),
                textStyle = HHType.bodyMd.copy(color = c.ink),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = c.accent, unfocusedBorderColor = c.stroke,
                    focusedContainerColor = c.surface, unfocusedContainerColor = c.surface,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            MonoLabel("Members · ${group.members.size}")
            group.members.forEach { member ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(34.dp).clip(CircleShape).background(c.bgElev),
                    ) { Text(member.user?.avatarEmoji?.ifBlank { "👤" } ?: "👤", fontSize = 16.sp) }
                    Text(member.user?.name ?: member.userId.take(8), style = HHType.bodySm, color = c.ink)
                }
            }

            vm.actionError?.let { Text(it, style = HHType.caption, color = Color(0xFFE2442F)) }

            Surface(
                onClick = {
                    if (!vm.isWorking) vm.renameGroup(group.id, name, emoji) { ok -> if (ok) onDismiss() }
                },
                shape = CircleShape, color = c.accent,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(if (vm.isWorking) "Saving…" else "Save changes", style = HHType.bodySm, fontWeight = FontWeight.Bold, color = c.onAccent)
                }
            }
            Surface(
                onClick = {
                    vm.leaveGroup(group.id, appState.currentUserId) { ok -> if (ok) onLeft() }
                },
                shape = CircleShape, color = c.surface, border = BorderStroke(1.dp, c.stroke),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Leave group", style = HHType.bodySm, fontWeight = FontWeight.Bold, color = Color(0xFFE2442F))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun EmojiPicker(selected: String, onSelect: (String) -> Unit) {
    val c = hh
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GROUP_EMOJIS.take(7).forEach { e ->
            Surface(
                onClick = { onSelect(e) },
                shape = CircleShape,
                color = if (e == selected) c.accentSoft else c.surface,
                border = BorderStroke(if (e == selected) 1.5.dp else 1.dp, if (e == selected) c.accent else c.stroke),
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) { Text(e, fontSize = 18.sp) }
            }
        }
    }
}

@Composable
private fun MemberToggleRow(user: HHUser, checked: Boolean, onToggle: () -> Unit) {
    val c = hh
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(HHRadius.md),
        color = if (checked) c.accentSoft else Color.Transparent,
        border = BorderStroke(1.dp, if (checked) c.accent else Color.Transparent),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AvatarView(user = user, size = 32.dp)
            Text(user.name, style = HHType.bodySm, fontWeight = FontWeight.SemiBold, color = c.ink, modifier = Modifier.weight(1f))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(20.dp).clip(CircleShape)
                    .background(if (checked) c.accent else Color.Transparent),
            ) {
                if (checked) Text("✓", fontSize = 12.sp, color = c.onAccent)
            }
        }
    }
}
