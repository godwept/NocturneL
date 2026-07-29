package ca.stewark.nocturnel.ui.theme

import androidx.compose.ui.graphics.Color

val TerminalBlack = Color(0xFF000000)
val TerminalBlackAlt = Color(0xFF050805)
val Phosphor = Color(0xFF00FF41)
val PhosphorDim = Color(0xFF00B32D)
val PhosphorMuted = Color(0xFF008020)
val PhosphorBright = Color(0xFF39FF7C)
val AlertAmber = Color(0xFFFFB000)
val TerminalError = Color(0xFFFF3030)
const val ScanlineAlpha = 0.18f

// Kept as a semantic alias for existing call sites.
val TerminalText = Phosphor
val TerminalPanel = TerminalBlackAlt
