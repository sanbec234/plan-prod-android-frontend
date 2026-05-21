package com.hanghub.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hanghub.app.ToastData
import com.hanghub.app.core.appContainer
import com.hanghub.app.core.viewModelFactory
import com.hanghub.app.data.*
import com.hanghub.app.ui.chrome.LocalAppChrome
import com.hanghub.app.ui.maps.ScoutMapboxMap
import com.hanghub.app.ui.maps.ScoutPlacePin
import com.hanghub.app.ui.maps.ScoutUserPin
import com.hanghub.app.ui.components.*
import com.hanghub.app.ui.state.LocalAppState
import com.hanghub.app.ui.theme.*
import kotlin.math.*

// ═══════════════════════════════════════════════════════════════════════════
// MARK: ScoutScreen
// ═══════════════════════════════════════════════════════════════════════════

enum class ScoutMetaphor { MAP, RADAR, STACK }

@Composable
fun ScoutScreen(
    onCreatePlan: () -> Unit,
    showToast: (ToastData) -> Unit,
) {
    val c = hh
    val appState = LocalAppState.current
    val container = appContainer()
    val scoutVm: ScoutViewModel = viewModel(
        factory = viewModelFactory {
            ScoutViewModel(
                container.discoveryRepository,
                container.profileRepository,
                container.locationService,
            )
        }
    )
    var metaphor by remember { mutableStateOf(ScoutMetaphor.MAP) }
    var selectedPlace by remember { mutableStateOf<HHPlace?>(null) }
    var showPlaceDetail by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result -> scoutVm.onPermissionResult(result.values.any { it }) }

    LaunchedEffect(Unit) {
        if (!scoutVm.hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )
            )
        }
    }
    LaunchedEffect(scoutVm.error) {
        scoutVm.error?.let { showToast(ToastData(it)) }
    }

    Box(modifier = Modifier.fillMaxSize().background(c.bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Header ─────────────────────────────────────────────────
            ScoutHeader(metaphor = metaphor, onMetaphorChange = { metaphor = it })

            // ── Map area ───────────────────────────────────────────────
            Box(modifier = Modifier.weight(1f)) {
                when (metaphor) {
                    ScoutMetaphor.MAP   -> ScoutMapView(
                        friends = appState.friends,
                        places = scoutVm.places,
                        onSelectPlace = { selectedPlace = it; showPlaceDetail = true },
                        onSelectUser  = { u -> showToast(ToastData("${u.name} is ${u.status.label}")) }
                    )
                    ScoutMetaphor.RADAR -> ScoutRadarView(
                        friends = appState.friends,
                        onSelectUser = { u -> showToast(ToastData("${u.name} is ${u.status.label}")) }
                    )
                    ScoutMetaphor.STACK -> ScoutStackView(
                        places = scoutVm.places,
                        isLoading = scoutVm.isLoading,
                        onSelectPlace = { selectedPlace = it; showPlaceDetail = true }
                    )
                }

                // FAB
                if (metaphor != ScoutMetaphor.STACK) {
                    FloatingActionButton(
                        onClick = onCreatePlan,
                        containerColor = c.accent,
                        contentColor   = c.onAccent,
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(HHSpacing.lg)
                            .padding(bottom = HHSpacing.lg)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text("+", fontSize = 20.sp, color = c.onAccent, fontWeight = FontWeight.Bold)
                            Text("New hang", style = HHType.bodyMd, color = c.onAccent, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ── Discovery peek ──────────────────────────────────────────
            if (metaphor != ScoutMetaphor.STACK) {
                DiscoveryPeek(count = scoutVm.places.size, onClick = onCreatePlan)
            }
        }

        // ── Place detail sheet ──────────────────────────────────────────
        AnimatedVisibility(
            visible = showPlaceDetail && selectedPlace != null,
            enter = slideInVertically { it } + fadeIn(),
            exit  = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            selectedPlace?.let { place ->
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f))
                            .clickable { showPlaceDetail = false }
                    )
                    PlaceDetailSheet(
                        place = place,
                        onClose = { showPlaceDetail = false },
                        onAddToPlan = { showPlaceDetail = false; onCreatePlan() },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}

// ── Scout header ──────────────────────────────────────────────────────────

@Composable
private fun ScoutHeader(metaphor: ScoutMetaphor, onMetaphorChange: (ScoutMetaphor) -> Unit) {
    val c = hh
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 18.dp, end = 18.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            MonoLabel("Scout · Mission District")
            Text("Who's around?", style = HHType.display(26), color = c.ink)
        }
        // metaphor switcher
        Surface(
            shape = RoundedCornerShape(HHRadius.md),
            color = c.surface,
            border = BorderStroke(1.dp, c.stroke),
        ) {
            Row(modifier = Modifier.padding(3.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                ScoutMetaphor.entries.forEach { m ->
                    val active = m == metaphor
                    Surface(
                        onClick = { onMetaphorChange(m) },
                        shape = RoundedCornerShape(HHRadius.sm),
                        color = if (active) c.accent else Color.Transparent,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                m.name.first().toString(),
                                style = HHType.mono,
                                color = if (active) c.onAccent else c.inkMute,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Discovery peek ────────────────────────────────────────────────────────

@Composable
private fun DiscoveryPeek(count: Int, onClick: () -> Unit) {
    val c = hh
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(topStart = HHRadius.xl, topEnd = HHRadius.xl),
        color = c.surface,
        border = BorderStroke(1.dp, c.stroke),
        shadowElevation = 12.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 14.dp)
                    .width(40.dp).height(4.dp)
                    .clip(CircleShape)
                    .background(c.strokeHi)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    MonoLabel("Discover")
                    Text(
                        if (count > 0) "$count spots nearby" else "Finding spots nearby…",
                        style = HHType.display(19),
                        color = c.ink,
                    )
                }
                Text("→", fontSize = 20.sp, color = c.inkMute)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MARK: ScoutMapView — stylised neighbourhood map in Canvas
// ═══════════════════════════════════════════════════════════════════════════

// Stylised overlay positions — the Android Scout map is a designed canvas, not
// a geo-accurate projection, so friends/places are spread across fixed anchors.
private val PIN_ANCHORS: List<Pair<Float, Float>> = listOf(
    0.38f to 0.34f, 0.68f to 0.48f, 0.22f to 0.62f, 0.55f to 0.72f,
    0.80f to 0.30f, 0.14f to 0.30f, 0.72f to 0.78f, 0.45f to 0.46f,
    0.30f to 0.54f, 0.62f to 0.62f,
)

@Composable
fun ScoutMapView(
    friends: List<HHUser>,
    places: List<HHPlace>,
    onSelectPlace: (HHPlace) -> Unit,
    onSelectUser: (HHUser) -> Unit,
) {
    ScoutMapboxMap(
        userPins = friends.take(7).mapIndexed { i, user ->
            val (x, y) = PIN_ANCHORS[i % PIN_ANCHORS.size]
            ScoutUserPin(id = "user:${user.id}", user = user, xFrac = x, yFrac = y)
        },
        placePins = places.take(6).mapIndexed { i, place ->
            val (x, y) = PIN_ANCHORS[(i + 7) % PIN_ANCHORS.size]
            ScoutPlacePin(id = "place:${place.id}", place = place, xFrac = x, yFrac = y)
        },
        onUserTap = onSelectUser,
        onPlaceTap = onSelectPlace,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun PlacePill(place: HHPlace, onClick: () -> Unit) {
    val c = hh
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = c.surface,
        border = BorderStroke(1.dp, c.stroke),
        shadowElevation = 4.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
        ) {
            Text(place.emoji, fontSize = 14.sp)
            Text(
                place.name.split(" ").first(),
                style = HHType.caption,
                fontWeight = FontWeight.SemiBold,
                color = c.ink
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MARK: ScoutRadarView
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun ScoutRadarView(friends: List<HHUser>, onSelectUser: (HHUser) -> Unit) {
    val c = hh
    val users = friends.take(6)

    val sweepAnim = rememberInfiniteTransition(label = "sweep")
    val sweepAngle by sweepAnim.animateFloat(
        0f, 360f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "sweepAngle"
    )

    Box(modifier = Modifier.fillMaxSize().background(
        Brush.radialGradient(listOf(c.accentSoft.copy(alpha = 0.5f), c.bg))
    )) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2
            val cy = size.height / 2
            val maxR = minOf(cx, cy) * 0.85f

            // rings
            listOf(0.25f, 0.5f, 0.75f, 1.0f).forEach { frac ->
                drawCircle(
                    color = c.stroke.copy(alpha = 0.5f),
                    radius = maxR * frac,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
                )
            }

            // sweep
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(Color.Transparent, c.accent.copy(alpha = 0.3f), Color.Transparent),
                    center = Offset(cx, cy),
                ),
                startAngle = sweepAngle,
                sweepAngle = 40f,
                useCenter = true,
                topLeft = Offset(cx - maxR, cy - maxR),
                size = androidx.compose.ui.geometry.Size(maxR * 2, maxR * 2),
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val cx = maxWidth / 2
            val cy = maxHeight / 2

            users.forEachIndexed { i, user ->
                val angle = (i.toDouble() / users.size) * Math.PI * 2 - Math.PI / 2
                val r = 80 + (user.distance ?: 1.0) * 38
                val x = cx + (cos(angle) * r).dp - 22.dp
                val y = cy + (sin(angle) * r).dp - 28.dp

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .absoluteOffset(x = x, y = y)
                        .clickable { onSelectUser(user) }
                ) {
                    AvatarView(user = user, size = 38.dp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        user.name.split(" ").first(),
                        style = HHType.caption,
                        fontWeight = FontWeight.SemiBold,
                        color = c.ink,
                    )
                }
            }

            // YOU
	            Box(
	                contentAlignment = Alignment.Center,
	                modifier = Modifier
	                    .absoluteOffset(x = cx - 11.dp, y = cy - 11.dp)
	                    .size(22.dp)
	                    .clip(CircleShape)
	                    .background(c.accent)
	                    .border(3.dp, c.bg, CircleShape)
	            ) {}
	        }
	    }
	}

// ═══════════════════════════════════════════════════════════════════════════
// MARK: ScoutStackView — swipeable place cards
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun ScoutStackView(
    places: List<HHPlace>,
    isLoading: Boolean,
    onSelectPlace: (HHPlace) -> Unit,
) {
    val c = hh
    if (places.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                if (isLoading) "Finding places nearby…" else "No places found nearby.",
                style = HHType.bodySm,
                color = c.inkMute,
            )
        }
        return
    }
    var topIndex by remember { mutableIntStateOf(0) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }

    val animDragX by animateFloatAsState(dragOffsetX, label = "dragX")

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(modifier = Modifier.weight(1f)) {
            (2 downTo 0).forEach { offset ->
                val idx = (topIndex + offset) % places.size
                val place = places[idx]
                val dark  = c.isDark
                val isTop = offset == 0
                val scale = 1f - offset * 0.04f
                val dy    = (offset * 10).dp
                val dx    = if (isTop) animDragX.dp else 0.dp
                val rot   = if (isTop) animDragX / 20f else 0f

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(x = dx, y = dy)
                        .scale(scale)
                        .rotate(rot)
                        .clip(RoundedCornerShape(HHRadius.xl))
                        .background(place.vibe.bg(dark))
                        .border(1.dp, c.stroke, RoundedCornerShape(HHRadius.xl))
                        .then(
                            if (isTop) Modifier
                                .pointerInput(topIndex) {
                                    detectDragGestures(
                                        onDragEnd = {
                                            if (abs(dragOffsetX) > 250f) {
                                                topIndex = (topIndex + 1) % places.size
                                            }
                                            dragOffsetX = 0f
                                        },
                                        onDragCancel = { dragOffsetX = 0f },
                                        onDrag = { _, drag -> dragOffsetX += drag.x }
                                    )
                                }
                                .clickable { onSelectPlace(place) }
                            else Modifier
                        )
                        .padding(22.dp)
                ) {
                    // card content
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Surface(shape = CircleShape, color = c.surface) {
                                Text(
                                    "${place.match}% match",
                                    style = HHType.monoXs,
                                    color = c.ink,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                            Text(place.emoji, fontSize = 48.sp)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(place.name, style = HHType.display(32), color = place.vibe.ink(dark), maxLines = 2)
                            Text(
                                "${place.category} · ${String.format("%.1f", place.dist)}mi · ${place.price}",
                                style = HHType.bodySm,
                                color = place.vibe.ink(dark).copy(alpha = 0.75f)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(shape = CircleShape, color = c.surface) {
                                    Text("★ ${place.rating}", style = HHType.caption, fontWeight = FontWeight.SemiBold, color = place.vibe.ink(dark), modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
                                }
                                if (place.friends > 0) {
                                    Surface(shape = CircleShape, color = c.surface) {
                                        Text("${place.friends} friends been here", style = HHType.caption, fontWeight = FontWeight.SemiBold, color = place.vibe.ink(dark), modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // action buttons
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.weight(1f))
            Surface(
                onClick = { dragOffsetX = -500f; topIndex = (topIndex + 1) % places.size; dragOffsetX = 0f },
                shape = CircleShape,
                color = c.surface,
                border = BorderStroke(1.dp, c.stroke),
                modifier = Modifier.size(56.dp)
            ) { Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Text("✕", fontSize = 20.sp, color = c.inkMute) } }
            Spacer(Modifier.width(20.dp))
            Surface(
                onClick = { dragOffsetX = 500f; topIndex = (topIndex + 1) % places.size; dragOffsetX = 0f },
                shape = CircleShape,
                color = c.accent,
                shadowElevation = 8.dp,
                modifier = Modifier.size(64.dp)
            ) { Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Text("♥", fontSize = 24.sp, color = c.onAccent) } }
            Spacer(Modifier.weight(1f))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MARK: PlaceDetailSheet
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun PlaceDetailSheet(
    place: HHPlace,
    onClose: () -> Unit,
    onAddToPlan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = hh
    val dark = c.isDark
    val chrome = LocalAppChrome.current

    // Hide the floating tab bar while this detail sheet is shown
    DisposableEffect(Unit) {
        val pop = chrome.pushTabBarHidden()
        onDispose { pop() }
    }

    Surface(
        shape = RoundedCornerShape(topStart = HHRadius.xl, topEnd = HHRadius.xl),
        color = c.bg,
        modifier = modifier.fillMaxSize(),
        shadowElevation = 32.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // handle
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 12.dp)
                    .width(40.dp).height(4.dp)
                    .clip(CircleShape)
                    .background(c.strokeHi)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 8.dp)
            ) {
                // Hero
                Box(
                    modifier = Modifier
                        .padding(horizontal = 18.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(HHRadius.xl))
                        .background(place.vibe.bg(dark))
                        .padding(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 40.dp, y = (-40).dp)
                            .clip(CircleShape)
                            .background(place.vibe.ink(dark).copy(alpha = 0.1f))
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(shape = CircleShape, color = c.surface) {
                                Text("${place.match}% match · ${place.vibe.label}", style = HHType.monoXs, color = place.vibe.ink(dark), modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                            }
                            Text(place.name, style = HHType.display(28), color = place.vibe.ink(dark))
                            Text(place.category, style = HHType.bodySm, color = place.vibe.ink(dark).copy(alpha = 0.75f))
                        }
                        Text(place.emoji, fontSize = 56.sp)
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Stats
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    listOf(
                        "RATING" to "★ ${place.rating}",
                        "AWAY"   to "${String.format("%.1f", place.dist)} mi",
                        "PRICE"  to place.price,
                    ).forEachIndexed { i, (label, value) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f).padding(vertical = 10.dp)
                        ) {
                            MonoLabel(label)
                            Spacer(Modifier.height(3.dp))
                            Text(value, style = HHType.bodyMd, fontWeight = FontWeight.Bold, color = c.ink)
                        }
                        if (i < 2) VerticalDivider(modifier = Modifier.height(34.dp).align(Alignment.CenterVertically), color = c.stroke)
                    }
                }

                // Hours row
                HHCard(modifier = Modifier.padding(horizontal = 18.dp).padding(top = 14.dp), padding = PaddingValues(14.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(60.dp).clip(RoundedCornerShape(HHRadius.md)).background(c.bgElev)
                        ) { Text("📍", fontSize = 22.sp) }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("447 Valencia St", style = HHType.bodyMd, fontWeight = FontWeight.SemiBold, color = c.ink)
                            Text(place.hours, style = HHType.caption, color = c.statusFree)
                        }
                    }
                }

                // CTAs
                Row(
                    modifier = Modifier.padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HHButton("Directions", style = HHButtonStyle.SECONDARY, onClick = {})
                    HHButton("Add to plan", style = HHButtonStyle.PRIMARY, fullWidth = true, onClick = onAddToPlan)
                }
            }
        }
    }
}
