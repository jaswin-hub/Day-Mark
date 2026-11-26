package com.daymark.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun BalanceScaleMark(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    color: androidx.compose.ui.graphics.Color = LocalDaymarkPalette.current.accent,
    tiltDegrees: Float = 0f,
    description: String = "Daymark balance scale"
) {
    Canvas(modifier.size(size).semantics { contentDescription = description }) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * .055f
        val centerX = w * .5f
        val beamY = h * .24f
        val panY = h * .72f
        val panTop = h * .58f
        val cap = StrokeCap.Round
        drawLine(color, Offset(centerX, h * .16f), Offset(centerX, h * .82f), stroke, cap)
        drawLine(color, Offset(centerX, h * .80f), Offset(w * .32f, h * .88f), stroke, cap)
        drawLine(color, Offset(centerX, h * .80f), Offset(w * .68f, h * .88f), stroke, cap)
        drawLine(color, Offset(w * .32f, h * .88f), Offset(w * .68f, h * .88f), stroke, cap)
        rotate(tiltDegrees, pivot = Offset(centerX, beamY)) {
            drawLine(color, Offset(w * .27f, beamY), Offset(w * .73f, beamY), stroke, cap)
            drawLine(color, Offset(w * .28f, beamY), Offset(w * .22f, panTop), stroke * .8f, cap)
            drawLine(color, Offset(w * .72f, beamY), Offset(w * .78f, panTop), stroke * .8f, cap)
            drawArc(color, 0f, 180f, false, Offset(w * .07f, panTop), androidx.compose.ui.geometry.Size(w * .30f, h * .20f), style = Stroke(width = stroke, cap = cap))
            drawArc(color, 0f, 180f, false, Offset(w * .63f, panTop), androidx.compose.ui.geometry.Size(w * .30f, h * .20f), style = Stroke(width = stroke, cap = cap))
        }
    }
}
