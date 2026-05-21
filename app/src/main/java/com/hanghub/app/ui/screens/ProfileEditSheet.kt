package com.hanghub.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hanghub.app.core.appContainer
import com.hanghub.app.core.viewModelFactory
import com.hanghub.app.ui.state.LocalAppState
import com.hanghub.app.ui.theme.HHRadius
import com.hanghub.app.ui.theme.HHType
import com.hanghub.app.ui.theme.hh

/** Edit display name + @handle. Wired to PATCH /api/v1/profile/me. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ProfileEditSheet(
    currentName: String,
    currentHandle: String,
    onDismiss: () -> Unit,
) {
    val c = hh
    val appState = LocalAppState.current
    val container = appContainer()
    val vm: ProfileViewModel = viewModel(
        factory = viewModelFactory { ProfileViewModel(appState, container.profileRepository) }
    )
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf(currentName) }
    var handle by remember { mutableStateOf(currentHandle) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = c.bg) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Edit profile", style = HHType.display(24), color = c.ink)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Display name", style = HHType.bodySm, color = c.inkDim) },
                singleLine = true,
                shape = RoundedCornerShape(HHRadius.lg),
                textStyle = HHType.bodyMd.copy(color = c.ink),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = c.accent, unfocusedBorderColor = c.stroke,
                    focusedContainerColor = c.surface, unfocusedContainerColor = c.surface,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = handle,
                onValueChange = { input -> handle = input.lowercase().filter { it.isLetterOrDigit() || it == '_' } },
                placeholder = { Text("@handle", style = HHType.bodySm, color = c.inkDim) },
                singleLine = true,
                shape = RoundedCornerShape(HHRadius.lg),
                textStyle = HHType.bodyMd.copy(color = c.ink),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = c.accent, unfocusedBorderColor = c.stroke,
                    focusedContainerColor = c.surface, unfocusedContainerColor = c.surface,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            vm.saveError?.let { Text(it, style = HHType.caption, color = Color(0xFFE2442F)) }

            Surface(
                onClick = {
                    if (!vm.isSaving) {
                        vm.updateProfile(name, handle) { ok -> if (ok) onDismiss() }
                    }
                },
                shape = CircleShape,
                color = c.accent,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        if (vm.isSaving) "Saving…" else "Save",
                        style = HHType.bodyMd, fontWeight = FontWeight.Bold, color = c.onAccent,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
