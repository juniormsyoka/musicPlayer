package com.vybzvault.music.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.cos
import kotlin.math.sin

/**
 * Custom shape for bottom navigation with a cut-out curve
 * Creates a modern floating effect with a smooth dip for the selected item
 */
class CutOutNavigationShape(
    private val cutOutRadius: Float = 80f,  // Size of the cut-out
    private val cutOutElevation: Float = 0f, // Vertical position of cut-out
    private val cornerRadius: Float = 28f   // Corner radius of the bar
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return Outline.Generic(
            path = createCutOutPath(size)
        )
    }

    private fun createCutOutPath(size: Size): Path {
        val path = Path()
        val cutOutCenterX = size.width / 2  // Center of the cut-out
        val cutOutY = cutOutElevation // Top of the cut-out dip
        val cutOutRadiusPx = cutOutRadius

        // Start from top-left corner
        path.moveTo(cornerRadius, 0f)

        // Top edge with cut-out
        path.lineTo(cutOutCenterX - cutOutRadiusPx, 0f)

        // Create the curved dip (cut-out)
        path.quadraticTo(
            cutOutCenterX - cutOutRadiusPx * 0.5f, cutOutY,
            cutOutCenterX, cutOutY + cutOutRadiusPx
        )
        path.quadraticTo(
            cutOutCenterX + cutOutRadiusPx * 0.5f, cutOutY,
            cutOutCenterX + cutOutRadiusPx, 0f
        )

        // Continue to top-right corner
        path.lineTo(size.width - cornerRadius, 0f)

        // Top-right corner arc
        path.arcTo(
            rect = androidx.compose.ui.geometry.Rect(
                size.width - cornerRadius * 2,
                0f,
                size.width,
                cornerRadius * 2
            ),
            startAngleDegrees = -90f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false
        )

        // Right edge
        path.lineTo(size.width, size.height - cornerRadius)

        // Bottom-right corner
        path.arcTo(
            rect = androidx.compose.ui.geometry.Rect(
                size.width - cornerRadius * 2,
                size.height - cornerRadius * 2,
                size.width,
                size.height
            ),
            startAngleDegrees = 0f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false
        )

        // Bottom edge
        path.lineTo(cornerRadius, size.height)

        // Bottom-left corner
        path.arcTo(
            rect = androidx.compose.ui.geometry.Rect(
                0f,
                size.height - cornerRadius * 2,
                cornerRadius * 2,
                size.height
            ),
            startAngleDegrees = 90f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false
        )

        // Left edge
        path.lineTo(0f, cornerRadius)

        // Top-left corner
        path.arcTo(
            rect = androidx.compose.ui.geometry.Rect(
                0f,
                0f,
                cornerRadius * 2,
                cornerRadius * 2
            ),
            startAngleDegrees = 180f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false
        )

        path.close()
        return path
    }
}

/**
 * Creates a sleek, modern navigation bar with a floating effect
 */
class FloatingNavigationShape(
    private val cornerRadius: Float = 32f,
    private val cutOutRadius: Float = 60f
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return Outline.Generic(createFloatingPath(size))
    }

    private fun createFloatingPath(size: Size): Path {
        val path = Path()
        val margin = 16f
        val innerWidth = size.width - (margin * 2)
        val innerHeight = size.height - (margin * 2)

        // Create a pill-shaped bar that floats above the bottom
        path.addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                left = margin,
                top = margin,
                right = size.width - margin,
                bottom = size.height - margin,
                radiusX = cornerRadius,
                radiusY = cornerRadius
            )
        )

        return path
    }
}