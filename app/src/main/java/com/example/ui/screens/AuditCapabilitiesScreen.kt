package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.CriticalRed
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.WarningAmber

@Composable
fun AuditCapabilitiesScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Feasibility Assessment Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NeonEmerald.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = NeonEmerald)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "FEASIBILITY: NON-ROOT AUTOMATION",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonEmerald,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = "100% Verified Feasible via Hybrid Architecture",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Android's Accessibility API does not require root privileges and allows inspecting view hierarchies across all third-party apps, extracting text/IDs, and executing programmatic node clicks. Starting from Android 7.0 (API 24), dispatchGesture() enables arbitrary coordinate-based taps, swipes, and multi-touch strokes without root. When combined with our local/wireless ADB bridge fallback, full unrestricted UI automation is accomplished.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 2. Android 4.4 to Android 15 OS Matrix
        item {
            Text(
                text = "OS COMPATIBILITY MATRIX (ANDROID 4.4 → ANDROID 15)",
                style = MaterialTheme.typography.labelSmall,
                color = CyberCyan,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
        }

        item {
            CompatibilityTierCard(
                title = "Android 7.0 – Android 10 (API 24–29) [OPTIMAL FOR OLD PHONES]",
                supportLevel = "Tier 1: Full Native Support",
                supportColor = NeonEmerald,
                features = listOf(
                    "dispatchGesture() native coordinate taps & swipes without root",
                    "Full AccessibilityNodeInfo inspection & node actions",
                    "Wireless ADB on port 5555 without pairing code hassles",
                    "Perfect for repurposing older hardware (Nexus 5X/6P, Galaxy S7/S8/S9, Pixel 1-3)"
                )
            )
        }

        item {
            CompatibilityTierCard(
                title = "Android 11 – Android 13 (API 30–33) [MODERN WORKERS]",
                supportLevel = "Tier 1: Full Support",
                supportColor = CyberCyan,
                features = listOf(
                    "Wireless Debugging with Pairing Code (adb pair)",
                    "Tightened background activity launch rules handled via foreground service",
                    "Enhanced accessibility event streaming & window change hooks"
                )
            )
        }

        item {
            CompatibilityTierCard(
                title = "Android 14 – Android 15 (API 34–36) [LATEST FLEET]",
                supportLevel = "Tier 1: Full Support with Guardrail",
                supportColor = ElectricBlue,
                features = listOf(
                    "Requires tapping 'Allow restricted settings' in App Info for sideloaded accessibility apps",
                    "Full Predictive Back & Edge-to-Edge window insets",
                    "Advanced memory isolation & modern TLS 1.3 cipher suites"
                )
            )
        }

        item {
            CompatibilityTierCard(
                title = "Android 4.4 – Android 6.0 (API 19–23) [LEGACY HARDWARE]",
                supportLevel = "Tier 2: Bridge Fallback Required",
                supportColor = WarningAmber,
                features = listOf(
                    "Can inspect hierarchy and click clickable Accessibility nodes",
                    "Lacks dispatchGesture() API: Coordinate taps and swipes must route through ADB bridge ('input tap / swipe')",
                    "Requires TLS 1.2 backports for encrypted rsyslog transmission"
                )
            )
        }

        // 3. Fallback Hierarchy Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "4-TIER MULTI-LAYER FALLBACK ARCHITECTURE",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    FallbackLayerRow(
                        level = "1",
                        name = "Accessibility Node Action",
                        desc = "Direct AccessibilityNodeInfo.performAction(ACTION_CLICK / SET_TEXT). Zero latency, event-driven.",
                        color = NeonEmerald
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FallbackLayerRow(
                        level = "2",
                        name = "Gesture Coordinate Dispatch",
                        desc = "Path stroke dispatched via AccessibilityService.dispatchGesture(). Taps exact pixel center if node is unclickable.",
                        color = CyberCyan
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FallbackLayerRow(
                        level = "3",
                        name = "Wireless / Local ADB Shell",
                        desc = "Executes 'input tap / swipe / text / keyevent' over local wireless loopback or remote ADB server.",
                        color = WarningAmber
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FallbackLayerRow(
                        level = "4",
                        name = "CV & Geometry Resolution",
                        desc = "Calculates screen bounding-box coordinates when elements are rendered inside Flutter / Canvas / OpenGL views.",
                        color = ElectricBlue
                    )
                }
            }
        }

        // 4. Capacitor Bridge & Rsyslog Encryption Summary
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = CyberCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Capacitor Bridge & Telemetry Encryption",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Capacitor Plugin Wrapper: Exposes inspectHierarchy(), click(), inputText(), swipe(), and executeWorkflow() as standard async JSON plugin methods for web agents.\n• Encrypted Telemetry: RFC 5424 syslog frames sent over TLS (SSLSocket) with offline buffering queue ensuring zero data loss during network drops.\n• Lightweight Execution: Pure Kotlin coroutines with structured concurrency, zero heavy background polling, minimal battery impact on legacy devices.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun CompatibilityTierCard(
    title: String,
    supportLevel: String,
    supportColor: Color,
    features: List<String>
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(supportColor.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = supportLevel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = supportColor)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            features.forEach { feat ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = supportColor,
                        modifier = Modifier
                            .size(14.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = feat,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun FallbackLayerRow(level: String, name: String, desc: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = level, fontWeight = FontWeight.Bold, color = color, fontSize = 11.sp)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
    }
}
