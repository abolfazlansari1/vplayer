package com.vplayer.app

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size

val Bg = Color(0xFF0B0D12)
val PanelBg = Color(0xFF141821)
val Card = Color(0xFF1A1F2B)
val Accent = Color(0xFF3D8BFF)
val TextPrimary = Color(0xFFE9ECF3)
val TextSecondary = Color(0xFF8F98AB)

private val scheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    background = Bg,
    onBackground = TextPrimary,
    surface = PanelBg,
    onSurface = TextPrimary,
    surfaceVariant = Card,
    onSurfaceVariant = TextSecondary,
)

@Composable
fun VPlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}

enum class Glyph { PLAY, PAUSE, NEXT, PREV, REWIND, FORWARD, FOLDER }

@Composable
fun GlyphIcon(
    glyph: Glyph,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    iconSize: Dp = 24.dp,
) {
    Canvas(modifier.size(iconSize)) {
        val w = this.size.width
        val h = this.size.height
        when (glyph) {
            Glyph.PLAY -> {
                val p = Path()
                p.moveTo(w * 0.28f, h * 0.15f)
                p.lineTo(w * 0.86f, h * 0.5f)
                p.lineTo(w * 0.28f, h * 0.85f)
                p.close()
                drawPath(p, tint)
            }
            Glyph.PAUSE -> {
                drawRoundRect(tint, Offset(w * 0.22f, h * 0.15f), Size(w * 0.2f, h * 0.7f), CornerRadius(w * 0.05f))
                drawRoundRect(tint, Offset(w * 0.58f, h * 0.15f), Size(w * 0.2f, h * 0.7f), CornerRadius(w * 0.05f))
            }
            Glyph.NEXT -> {
                val p = Path()
                p.moveTo(w * 0.15f, h * 0.2f)
                p.lineTo(w * 0.68f, h * 0.5f)
                p.lineTo(w * 0.15f, h * 0.8f)
                p.close()
                drawPath(p, tint)
                drawRoundRect(tint, Offset(w * 0.74f, h * 0.2f), Size(w * 0.12f, h * 0.6f), CornerRadius(w * 0.03f))
            }
            Glyph.PREV -> {
                val p = Path()
                p.moveTo(w * 0.85f, h * 0.2f)
                p.lineTo(w * 0.32f, h * 0.5f)
                p.lineTo(w * 0.85f, h * 0.8f)
                p.close()
                drawPath(p, tint)
                drawRoundRect(tint, Offset(w * 0.14f, h * 0.2f), Size(w * 0.12f, h * 0.6f), CornerRadius(w * 0.03f))
            }
            Glyph.REWIND -> {
                val p = Path()
                p.moveTo(w * 0.5f, h * 0.22f)
                p.lineTo(w * 0.1f, h * 0.5f)
                p.lineTo(w * 0.5f, h * 0.78f)
                p.close()
                p.moveTo(w * 0.9f, h * 0.22f)
                p.lineTo(w * 0.5f, h * 0.5f)
                p.lineTo(w * 0.9f, h * 0.78f)
                p.close()
                drawPath(p, tint)
            }
            Glyph.FORWARD -> {
                val p = Path()
                p.moveTo(w * 0.5f, h * 0.22f)
                p.lineTo(w * 0.9f, h * 0.5f)
                p.lineTo(w * 0.5f, h * 0.78f)
                p.close()
                p.moveTo(w * 0.1f, h * 0.22f)
                p.lineTo(w * 0.5f, h * 0.5f)
                p.lineTo(w * 0.1f, h * 0.78f)
                p.close()
                drawPath(p, tint)
            }
            Glyph.FOLDER -> {
                drawRoundRect(tint, Offset(w * 0.08f, h * 0.2f), Size(w * 0.4f, h * 0.2f), CornerRadius(w * 0.06f))
                drawRoundRect(tint, Offset(w * 0.08f, h * 0.3f), Size(w * 0.84f, h * 0.52f), CornerRadius(w * 0.08f))
            }
        }
    }
}
