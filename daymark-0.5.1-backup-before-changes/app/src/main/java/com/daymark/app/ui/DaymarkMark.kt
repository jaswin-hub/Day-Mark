package com.daymark.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** A geometric D joined to a timeline and its selected date marker. */
@Composable
fun DaymarkMark(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    description: String = "Daymark date marker"
) {
    val palette = LocalDaymarkPalette.current
    Canvas(modifier.size(size).semantics { contentDescription = description }) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * .09f
        val mark = Path().apply {
            moveTo(w * .23f, h * .16f)
            lineTo(w * .23f, h * .84f)
            lineTo(w * .49f, h * .84f)
            cubicTo(w * .83f, h * .84f, w * .83f, h * .16f, w * .49f, h * .16f)
            close()
        }
        drawPath(mark, palette.textPrimary, style = Stroke(width = stroke, cap = StrokeCap.Round))
        val y = h * .72f
        drawLine(palette.accent, Offset(w * .32f, y), Offset(w * .78f, y), strokeWidth = stroke * .58f, cap = StrokeCap.Round)
        drawCircle(palette.background, radius = w * .14f, center = Offset(w * .66f, y))
        drawCircle(palette.accent, radius = w * .105f, center = Offset(w * .66f, y))
    }
}
