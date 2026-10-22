package app.samba.island

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

object SambaColors {
    val Ink = Color(0xFF08090D)
    val Neon = Color(0xFF42D9FF)
    val Violet = Color(0xFF8C63FF)
    val Mint = Color(0xFF48E6C1)
    val Muted = Color(0xFF8D93A6)
    val Line = Color(0xFF1E2230)
}

/** Island di dalam overlay. Dipasang oleh IslandService. */
@Composable
fun IslandHost() {
    val cfg by SambaStore.config.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(expanded) {
        if (expanded) {
            delay(4000)
            expanded = false
        }
    }

    IslandPill(
        compactWidth = cfg.width.dp,
        compactHeight = cfg.height.dp,
        expanded = expanded,
        onClick = { expanded = !expanded },
    )
}

/** Pil island: ukuran dianimasikan dengan spring, isi berganti dengan crossfade. */
@Composable
fun IslandPill(
    compactWidth: Dp,
    compactHeight: Dp,
    expanded: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val expandedWidth = (LocalConfiguration.current.screenWidthDp - 32).coerceAtMost(300).dp
    val w by animateDpAsState(
        targetValue = if (expanded) expandedWidth else compactWidth,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
        label = "islandWidth",
    )
    val h by animateDpAsState(
        targetValue = if (expanded) 68.dp else compactHeight,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
        label = "islandHeight",
    )
    val shape = RoundedCornerShape(h / 2)

    Box(
        modifier = modifier
            .size(w, h)
            .clip(shape)
            .background(SambaColors.Ink)
            .border(1.dp, Brush.horizontalGradient(listOf(SambaColors.Neon, SambaColors.Violet)), shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(targetState = expanded, label = "islandContent") { isExpanded ->
            if (isExpanded) ExpandedContent() else CompactContent(compactHeight)
        }
    }
}

@Composable
private fun CompactContent(height: Dp) {
    val dot = height * 0.4f
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(dot).clip(CircleShape).background(SambaColors.Neon))
        Spacer(Modifier.width(height * 0.18f))
        Box(
            Modifier
                .size(width = dot * 1.9f, height = dot * 0.55f)
                .clip(CircleShape)
                .background(SambaColors.Violet)
        )
    }
}

@Composable
private fun ExpandedContent() {
    Row(
        modifier = Modifier.padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(SambaColors.Neon, SambaColors.Violet)))
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text("SAMBA", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Island aktif. Ketuk untuk menutup.",
                color = SambaColors.Muted,
                fontSize = 12.sp,
                maxLines = 1,
            )
        }
    }
}
