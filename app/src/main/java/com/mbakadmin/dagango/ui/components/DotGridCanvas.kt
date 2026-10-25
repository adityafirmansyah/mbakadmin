package com.mbakadmin.dagango.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.mbakadmin.dagango.ui.theme.AppDnaTokens

/**
 * Mandatory Pattern: `dot-grid-canvas`
 * 1px dot texture arranged on a 24px repeating tile grid over cool light canvas.
 */
fun Modifier.dotGridCanvas(): Modifier = this
    .drawBehind {
        // Fill canvas background
        drawRect(color = AppDnaTokens.SurfaceCanvas)

        val stepPx = 24.dp.toPx()
        val dotRadiusPx = 1.dp.toPx()
        val dotColor = AppDnaTokens.DotGridColor.copy(alpha = 0.55f)

        var x = 0f
        while (x < size.width) {
            var y = 0f
            while (y < size.height) {
                drawCircle(
                    color = dotColor,
                    radius = dotRadiusPx,
                    center = Offset(x, y)
                )
                y += stepPx
            }
            x += stepPx
        }
    }

@Composable
fun DotGridCanvasBox(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .dotGridCanvas()
            .padding(
                start = AppDnaTokens.MarginHorizontal,
                end = AppDnaTokens.MarginHorizontal,
                bottom = AppDnaTokens.ClearanceBottom
            ),
        content = content
    )
}
