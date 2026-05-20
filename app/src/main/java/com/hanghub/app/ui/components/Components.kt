package com.hanghub.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.hanghub.app.data.*
import com.hanghub.app.ui.theme.*

// ═══════════════════════════════════════════════════════════════════════════
// MARK: AvatarView
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun AvatarView(
    user: HHUser,
    size: Dp = 40.dp,
    showRing: Boolean = true,
) {
    val c = hh
    val ringSize = size + 6.dp

    // pulse animation for free status
    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnim.animateFloat(
        initialValue = 1f, targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearOutSlowInEasing), RepeatMode.Restart),
        label = "pulseScale"
    )
    val pulseAlpha by pulseAnim.animateFloat(
        initialValue = 0.6f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearOutSlowInEasing), RepeatMode.Restart),
        label = "pulseAlpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(ringSize)
    ) {
        // outer pulse ring (free only)
        if (showRing && user.status == UserStatus.FREE) {
            Box(
                modifier = Modifier
                    .size(ringSize)
                    .scale(pulseScale)
                    .alpha(pulseAlpha)
                    .border(2.dp, c.statusFree, CircleShape)
            )
            // solid ring
            Box(
                modifier = Modifier
                    .size(ringSize)
                    .border(2.dp, c.statusFree, CircleShape)
            )
        } else if (showRing && user.status == UserStatus.BUSY) {
            Box(
                modifier = Modifier
                    .size(ringSize)
                    .border(2.dp, c.statusBusy, CircleShape)
            )
        }

        // avatar bubble
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(c.accentSoft)
                .border(1.dp, c.stroke, CircleShape)
        ) {
            Text(user.avatar, fontSize = (size.value * 0.48).sp)
        }

        // status dot
        if (user.status != UserStatus.OFFLINE) {
            val dotColor = if (user.status == UserStatus.FREE) c.statusFree else c.statusBusy
            Box(
                modifier = Modifier
                    .size(size * 0.28f)
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .clip(CircleShape)
                    .background(c.bg)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MARK: VibeChip (small pill)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun VibeChip(
    vibe: HangVibe,
    selected: Boolean = false,
    compact: Boolean = false,
    onClick: () -> Unit = {},
) {
    val c = hh
    val dark = c.isDark
    val bg    = if (selected) vibe.bg(dark) else c.bgElev
    val fg    = if (selected) vibe.ink(dark) else c.inkMute
    val border= if (selected) Color.Transparent else c.stroke

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = bg,
        border = BorderStroke(1.dp, border),
        modifier = Modifier.animateContentSize(HHMotion.subtle())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 7.dp),
            modifier = Modifier.padding(
                horizontal = if (compact) 10.dp else 14.dp,
                vertical   = if (compact) 6.dp  else 8.dp,
            )
        ) {
            Text(vibe.emoji, fontSize = if (compact) 13.sp else 15.sp)
            Text(
                vibe.label,
                style = if (compact) HHType.caption else HHType.bodySm,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = fg,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MARK: VibeCard (big expressive card)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun VibeCard(
    vibe: HangVibe,
    selected: Boolean = false,
    height: Dp = 160.dp,
    onClick: () -> Unit,
) {
    val c = hh
    val dark = c.isDark
    val scale by animateFloatAsState(
        targetValue = if (selected) 0.97f else 1f,
        animationSpec = HHMotion.subtle(),
        label = "vibeCardScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .scale(scale)
            .clip(RoundedCornerShape(HHRadius.lg))
            .background(vibe.bg(dark))
            .border(
                width = 2.dp,
                color = if (selected) vibe.ink(dark) else Color.Transparent,
                shape = RoundedCornerShape(HHRadius.lg)
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        // decorative circle
        Box(
            modifier = Modifier
                .size(80.dp)
                .align(Alignment.TopEnd)
                .offset(x = 20.dp, y = (-20).dp)
                .clip(CircleShape)
                .background(vibe.ink(dark).copy(alpha = 0.1f))
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(vibe.emoji, fontSize = 40.sp)
            Text(
                vibe.label,
                style = HHType.display(22),
                color = vibe.ink(dark),
                maxLines = 1,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MARK: HHButton
// ═══════════════════════════════════════════════════════════════════════════

enum class HHButtonStyle { PRIMARY, SECONDARY, GHOST, DANGER }
enum class HHButtonSize  { SM, MD, LG }

@Composable
fun HHButton(
    label: String,
    style: HHButtonStyle = HHButtonStyle.PRIMARY,
    size: HHButtonSize   = HHButtonSize.MD,
    icon: @Composable (() -> Unit)? = null,
    fullWidth: Boolean = false,
    onClick: () -> Unit,
) {
    val c = hh
    val height = when (size) { HHButtonSize.SM -> 36.dp; HHButtonSize.LG -> 54.dp; else -> 46.dp }
    val fs     = when (size) { HHButtonSize.SM -> 14.sp; HHButtonSize.LG -> 17.sp; else -> 15.sp }
    val hPad   = when (size) { HHButtonSize.LG -> 22.dp; else -> 18.dp }

    val bg  = when (style) { HHButtonStyle.PRIMARY -> c.accent; HHButtonStyle.SECONDARY -> c.surface; else -> Color.Transparent }
    val fg  = when (style) { HHButtonStyle.PRIMARY -> c.onAccent; HHButtonStyle.DANGER -> Color(0xFFE2442F); else -> c.ink }
    val bdr = when (style) {
        HHButtonStyle.PRIMARY   -> Color.Transparent
        HHButtonStyle.SECONDARY -> c.stroke
        HHButtonStyle.GHOST     -> c.stroke
        HHButtonStyle.DANGER    -> Color(0xFFE2442F).copy(alpha = 0.33f)
    }

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = bg,
        border = BorderStroke(1.dp, bdr),
        shadowElevation = if (style == HHButtonStyle.PRIMARY) 4.dp else 0.dp,
        modifier = if (fullWidth) Modifier.fillMaxWidth().height(height)
                   else Modifier.height(height),
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = hPad)
        ) {
            if (icon != null) { icon(); Spacer(Modifier.width(8.dp)) }
            Text(label, style = HHType.body, fontSize = fs, fontWeight = FontWeight.SemiBold, color = fg)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MARK: AuraBadge
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun AuraBadge(points: Int, size: TextUnit = 11.sp) {
    val c = hh
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text("✦", style = HHType.mono, fontSize = size, color = c.accentInk.copy(alpha = 0.6f))
        Text("$points", style = HHType.mono, fontSize = size, color = c.accentInk)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MARK: LiveDot
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun LiveDot(color: Color, label: String) {
    val pulse = rememberInfiniteTransition(label = "liveDot")
    val scale by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse),
        label = "dotScale"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(color)
        )
        Text(label.uppercase(), style = HHType.monoXs, color = color)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MARK: MonoLabel
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun MonoLabel(text: String, color: Color? = null) {
    val c = hh
    Text(
        text.uppercase(),
        style = HHType.monoXs,
        color = color ?: c.inkDim,
        letterSpacing = 1.sp,
    )
}

// ═══════════════════════════════════════════════════════════════════════════
// MARK: TypingIndicator
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun TypingIndicator() {
    val c = hh
    val anim = rememberInfiniteTransition(label = "typing")
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        (0..2).forEach { i ->
            val offsetY by anim.animateFloat(
                initialValue = 0f, targetValue = -4f,
                animationSpec = infiniteRepeatable(
                    tween(500, delayMillis = i * 150, easing = FastOutSlowInEasing),
                    RepeatMode.Reverse
                ),
                label = "dot$i"
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .offset(y = offsetY.dp)
                    .clip(CircleShape)
                    .background(c.accent)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MARK: PlanCard
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun PlanCard(plan: HHPlan, onClick: () -> Unit) {
    val c = hh
    val dark = c.isDark
    val yesCount   = plan.rsvp.values.count { it == "yes" }
    val maybeCount = plan.rsvp.values.count { it == "maybe" }
    val isUrgent   = plan.inHrs < 2

    val breatheAnim = rememberInfiniteTransition(label = "breathe")
    val breatheScale by breatheAnim.animateFloat(
        initialValue = 1f, targetValue = 1.012f,
        animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breatheScale"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(HHRadius.xl),
        color = c.surface,
        border = BorderStroke(if (isUrgent) 1.5.dp else 1.dp, if (isUrgent) c.accent else c.stroke),
        shadowElevation = if (isUrgent) 8.dp else 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (isUrgent) breatheScale else 1f)
    ) {
        Column(modifier = Modifier.padding(HHSpacing.md)) {
            // top row
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // vibe icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(HHRadius.md))
                        .background(plan.vibe.bg(dark))
                ) {
                    Text(plan.vibe.emoji, fontSize = 26.sp)
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // badges row
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = plan.vibe.bg(dark),
                        ) {
                            Text(
                                plan.vibe.label.uppercase(),
                                style = HHType.monoXs,
                                color = plan.vibe.ink(dark),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            if (plan.state == PlanState.VOTING) "· VOTING" else "· CONFIRMED",
                            style = HHType.monoXs,
                            color = if (plan.state == PlanState.VOTING) c.inkDim else c.statusFree,
                        )
                    }
                    Text(plan.title, style = HHType.display(19), color = c.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("🕐", fontSize = 11.sp)
                        Text(
                            buildString {
                                append(plan.time)
                                plan.place?.let { append(" · ${it.name}") }
                            },
                            style = HHType.caption,
                            color = c.inkMute,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = c.stroke)

            // bottom row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // avatar stack
                    AvatarStack(userIDs = plan.participantIDs.take(4))
                    Text(
                        "$yesCount going${if (maybeCount > 0) " · $maybeCount maybe" else ""}",
                        style = HHType.caption,
                        color = c.inkMute
                    )
                }
                if (isUrgent) LiveDot(color = c.accent, label = "SOON")
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MARK: AvatarStack
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun AvatarStack(userIDs: List<String>, size: Dp = 26.dp) {
    val c = hh
    Box(modifier = Modifier.height(size + 4.dp)) {
        userIDs.take(4).forEachIndexed { i, uid ->
            val user = SampleData.user(uid) ?: return@forEachIndexed
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset(x = (i * (size.value * 0.65)).dp)
                    .size(size + 4.dp)
                    .clip(CircleShape)
                    .background(c.surface)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(c.accentSoft)
            ) {
                Text(user.avatar, fontSize = (size.value * 0.52).sp)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MARK: HHCard — surface with stroke
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun HHCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(HHSpacing.md),
    radius: Dp = HHRadius.lg,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = hh
    if (onClick != null) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(radius),
            color = c.surface,
            border = BorderStroke(1.dp, c.stroke),
            shadowElevation = 2.dp,
            modifier = modifier,
        ) {
            Column(modifier = Modifier.padding(padding), content = content)
        }
    } else {
        Surface(
            shape = RoundedCornerShape(radius),
            color = c.surface,
            border = BorderStroke(1.dp, c.stroke),
            shadowElevation = 2.dp,
            modifier = modifier,
        ) {
            Column(modifier = Modifier.padding(padding), content = content)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MARK: Shimmer modifier
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun Modifier.shimmer(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmerX by transition.animateFloat(
        initialValue = -1f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "shimmerX"
    )
    val c = hh
    return this.drawWithContent {
        drawContent()
        val brush = Brush.horizontalGradient(
            colors = listOf(Color.Transparent, c.stroke.copy(alpha = 0.5f), Color.Transparent),
            startX = shimmerX * size.width,
            endX   = (shimmerX + 1f) * size.width,
        )
        drawRect(brush)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MARK: SectionHeader
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun SectionHeader(title: String, count: Int? = null, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        MonoLabel(if (count != null) "$title · $count" else title)
        trailing?.invoke()
    }
}
