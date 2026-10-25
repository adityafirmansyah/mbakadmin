package com.mbakadmin.dagango.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Productivity Focus App DNA - Design Tokens
 * Reusable design contract defining colors, radii, spacing, elevations, and typography scale.
 */
object AppDnaTokens {
    // === COLOR PALETTE (CSS / Compose Token Bindings) ===
    val SurfaceCanvas = Color(0xFFF4F5F7)
    val DotGridColor = Color(0xFFD1D5DB)

    val SurfaceCard = Color(0xFFFFFFFF)
    val SurfaceMuted = Color(0xFFF0F1F3)

    val PrimaryHero = Color(0xFF181A1F)
    val OnPrimaryHero = Color(0xFFFFFFFF)

    val Foreground = Color(0xFF111318)
    val ForegroundMuted = Color(0xFF6E737D)
    val ForegroundSubtle = Color(0xFF9CA3AF)

    val BorderSubtle = Color(0x14000000) // ~8% black hairline
    val BorderCard = Color(0xFFE5E7EB)

    val AccentCyan = Color(0xFF06B6D4)
    val SuccessGreen = Color(0xFF10B981)
    val WarningAmber = Color(0xFFF59E0B)
    val RoseAlert = Color(0xFFF43F5E)

    // === CORNER RADII ===
    val RadiusPill = 8.dp
    val RadiusMicro = 12.dp
    val RadiusCard = 24.dp
    val RadiusHero = 32.dp
    val RadiusFab = 9999.dp // full-circle

    // === SPACING & GEOMETRY ===
    val MarginHorizontal = 20.dp
    val GapFeed = 12.dp
    val ClearanceBottom = 96.dp
    val FabSize = 44.dp
    val FabElevationOffset = 8.dp

    // === ELEVATIONS ===
    val ElevationLevel1 = 2.dp
    val ElevationLevel2 = 8.dp

    // === TYPOGRAPHY SCALES ===
    val TextMicro = 10.sp
    val TextEyebrow = 11.sp
    val TextCaption = 12.sp
    val TextBody = 14.sp
    val TextTitle = 16.sp
    val TextHero = 20.sp
    val TextNumeral24 = 24.sp
    val TextNumeral30 = 30.sp
    val TextNumeral36 = 36.sp
}
