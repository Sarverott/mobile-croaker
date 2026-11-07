package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Monochrome Terminal Palette (Black - Grey - White)
val TerminalBlack = Color(0xFF080808)
val TerminalBackground = Color(0xFF0D0D0D)
val TerminalSurface = Color(0xFF161616)
val TerminalSurfaceVariant = Color(0xFF222222)
val TerminalBorder = Color(0xFF333333)
val TerminalBorderLight = Color(0xFF444444)

val TerminalWhite = Color(0xFFFFFFFF)
val TerminalTextPrimary = Color(0xFFF3F4F6)
val TerminalTextSecondary = Color(0xFFA1A1AA)
val TerminalTextMuted = Color(0xFF71717A)

// Functional Status Markers
val StatusGreen = Color(0xFF22C55E)
val StatusAmber = Color(0xFFEAB308)
val StatusRed = Color(0xFFEF4444)
val StatusBlue = Color(0xFF38BDF8)

// Legacy alias compatibility
val CyberCyan = TerminalWhite
val CyberCyanMuted = TerminalTextSecondary
val ElectricBlue = StatusBlue
val NeonEmerald = StatusGreen
val WarningAmber = StatusAmber
val CriticalRed = StatusRed
