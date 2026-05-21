package com.hanghub.app.ui.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hanghub.app.core.AppContainer
import com.hanghub.app.core.viewModelFactory
import com.hanghub.app.data.dto.UserDto
import com.hanghub.app.ui.theme.HHRadius
import com.hanghub.app.ui.theme.HHType
import com.hanghub.app.ui.theme.hh

/**
 * Sign-in screen shown while the user is unauthenticated. Mirrors the iOS
 * `AuthView`: brand mark, tagline, and a single Google sign-in action.
 */
@Composable
fun AuthScreen(
    container: AppContainer,
    onAuthenticated: (UserDto) -> Unit,
) {
    val c = hh
    val vm: AuthViewModel = viewModel(
        factory = viewModelFactory {
            AuthViewModel(container.authRepository, container.googleAuth)
        }
    )
    val state = vm.uiState

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> vm.handleSignInResult(result.data) }

    LaunchedEffect(vm.signedInUser) {
        vm.signedInUser?.let(onAuthenticated)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg)
            .systemBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))

            // ── Brand mark ──────────────────────────────────────────────
            Text("HangHub", style = HHType.display(40), color = c.ink)
            Spacer(Modifier.height(10.dp))
            Text(
                "Make plans. Actually go.",
                style = HHType.bodySm,
                color = c.inkMute,
            )

            Spacer(Modifier.weight(1f))

            // ── Configuration hint ──────────────────────────────────────
            if (!vm.isGoogleConfigured) {
                Text(
                    "Setup needed — set default_web_client_id in strings.xml " +
                        "to enable Google Sign-In.",
                    style = HHType.caption,
                    color = c.inkMute,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 14.dp),
                )
            }

            // ── Google sign-in ──────────────────────────────────────────
            Surface(
                onClick = {
                    vm.beginLoading()
                    launcher.launch(container.googleAuth.signInIntent())
                },
                enabled = !state.isLoading,
                shape = RoundedCornerShape(HHRadius.lg),
                color = c.surface,
                border = BorderStroke(1.dp, c.stroke),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = c.ink,
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(
                        if (state.isLoading) "Signing in…" else "Continue with Google",
                        style = HHType.bodyMd,
                        fontWeight = FontWeight.SemiBold,
                        color = c.ink,
                    )
                }
            }

            // ── Error ───────────────────────────────────────────────────
            state.error?.let { message ->
                Spacer(Modifier.height(12.dp))
                Text(
                    message,
                    style = HHType.caption,
                    color = Color(0xFFE2442F),
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(48.dp))
        }
    }
}
