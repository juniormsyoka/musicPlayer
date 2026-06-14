package com.vybzvault.music.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Premium Navigation Item Structure
 */
data class ModernNavData(
    val id: String,
    val title: String,
    val icon: ImageVector
)

/**
 * Premium, fluid Island-Style Custom Navigation Bar.
 * This completely replaces standard Material3 NavigationBar rows with a highly tactile custom tracking indicator.
 */
@Composable
fun AnimatedNavigationBar(
    items: List<ModernNavData>,
    selectedId: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val density = LocalDensity.current
    val selectedIndex = remember(items, selectedId) { items.indexOfFirst { it.id == selectedId }.coerceAtLeast(0) }

    // Dynamic layout measurement states for fluid indicator tracking
    var maxRowWidth by remember { mutableIntStateOf(0) }
    val totalItemsCount = items.size

    // Calculate fractional position offset cleanly for layout transformations
    val targetFraction = selectedIndex.toFloat() / totalItemsCount
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy, // High premium fluid rebound factor
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "indicatorSlidingTrack"
    )

    // Main Floating Dock Container Layout
    Box(
        modifier = modifier
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(32.dp),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.3f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
            )
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
                    )
                ),
                shape = RoundedCornerShape(32.dp)
            )
            .clip(RoundedCornerShape(32.dp))
            .height(76.dp)
    ) {

        // Fluid Active Selection Pill Background (Dynamic Tracker Component)
        if (maxRowWidth > 0) {
            val itemWidthDp = with(density) { (maxRowWidth / totalItemsCount).toDp() }
            val totalHorizontalShiftDp = with(density) { maxRowWidth.toDp() }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .height(52.dp)
                    .size(width = itemWidthDp, height = 76.dp)
                    .graphicsLayer {
                        // Directly mutates visual position cleanly without forcing dirty UI recomposition loops
                        translationX = animatedFraction * totalHorizontalShiftDp.toPx()
                    }
                    .padding(horizontal = 8.dp, vertical = 10.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Interior micro-indicator glow dot
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp)
                        .size(5.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
            }
        }

        // Interactive Tab Layer Row
        Row(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    maxRowWidth = coordinates.size.width
                },
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex

                // Micro tactile pop animation configs
                val contentScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.12f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "tactilePop"
                )

                val contentAlpha by animateFloatAsState(
                    targetValue = if (isSelected) 1.0f else 0.55f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "alphaFade"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null // Suppress crude native ripples to maintain sleek layout looks
                        ) {
                            onItemSelected(item.id)
                        },
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(24.dp)
                            .scale(contentScale)
                            .graphicsLayer { alpha = contentAlpha }
                    )

                    Text(
                        text = item.title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .scale(if (isSelected) 1f else 0.95f)
                            .graphicsLayer { alpha = contentAlpha }
                    )
                }
            }
        }
    }
}