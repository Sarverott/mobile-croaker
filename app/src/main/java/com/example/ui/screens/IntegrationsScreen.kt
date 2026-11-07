package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.hardware.DeviceHardwareBridge
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalMuted
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalWhite

@Composable
fun IntegrationsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val hw = remember { DeviceHardwareBridge.getInstance(context) }

    var isTorchOn by remember { mutableStateOf(hw.isFlashlightOn()) }
    var isSpeakerOn by remember { mutableStateOf(false) }
    var clipboardText by remember { mutableStateOf(hw.getClipboardText()) }
    var newClipInput by remember { mutableStateOf("") }
    var smsAddress by remember { mutableStateOf("") }
    var smsMessage by remember { mutableStateOf("") }
    var locationInfo by remember { mutableStateOf(hw.getLastKnownLocation().toString(2)) }
    var wifiInfo by remember { mutableStateOf(hw.getWifiStatus().toString(2)) }
    var btInfo by remember { mutableStateOf(hw.getBluetoothStatus().toString(2)) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TerminalBorder, RoundedCornerShape(8.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.hardware_title),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TerminalWhite
                    )
                    Text(
                        text = "Real-time hardware telemetry and manual hardware actuator test suite.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = TerminalMuted,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // 1. Torch & Physical Key Controls
        item {
            HwCard(title = "FLASHLIGHT & PHYSICAL BUTTON CONTROLS") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val next = !isTorchOn
                            if (hw.setFlashlight(next)) {
                                isTorchOn = next
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isTorchOn) StatusGreen else Color(0xFF252A30)
                        )
                    ) {
                        Text(
                            text = if (isTorchOn) "Torch ON" else "Torch OFF",
                            fontFamily = FontFamily.Monospace,
                            color = if (isTorchOn) Color.Black else TerminalWhite
                        )
                    }

                    Button(
                        onClick = {
                            val ok = hw.takeAccessibilityScreenshot()
                            Toast.makeText(context, if (ok) "Screenshot triggered" else "Screenshot action failed", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF252A30))
                    ) {
                        Text("Screenshot", fontFamily = FontFamily.Monospace, color = TerminalWhite)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { hw.adjustVolume(android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.ADJUST_RAISE) },
                        modifier = Modifier.weight(1f),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(TerminalBorder))
                    ) {
                        Text("Vol +", fontFamily = FontFamily.Monospace, color = TerminalWhite)
                    }

                    OutlinedButton(
                        onClick = { hw.adjustVolume(android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.ADJUST_LOWER) },
                        modifier = Modifier.weight(1f),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(TerminalBorder))
                    ) {
                        Text("Vol -", fontFamily = FontFamily.Monospace, color = TerminalWhite)
                    }

                    OutlinedButton(
                        onClick = { hw.pressPowerDialog() },
                        modifier = Modifier.weight(1.2f),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(TerminalBorder))
                    ) {
                        Text("Power Menu", fontFamily = FontFamily.Monospace, color = TerminalWhite)
                    }
                }
            }
        }

        // 2. Clipboard Management
        item {
            HwCard(title = "CLIPBOARD INTERCEPTOR & TYPING ACCELERATOR") {
                Text(
                    text = "Current Buffer: \"$clipboardText\"",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = TerminalWhite
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newClipInput,
                    onValueChange = { newClipInput = it },
                    label = { Text("Write to Clipboard", fontFamily = FontFamily.Monospace) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StatusGreen,
                        unfocusedBorderColor = TerminalBorder,
                        focusedTextColor = TerminalWhite,
                        unfocusedTextColor = TerminalWhite
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            if (newClipInput.isNotBlank()) {
                                hw.setClipboardText("MobileCroaker", newClipInput)
                                clipboardText = newClipInput
                                newClipInput = ""
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusGreen)
                    ) {
                        Text("Set Clipboard", fontFamily = FontFamily.Monospace, color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            clipboardText = hw.getClipboardText()
                        },
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(TerminalBorder))
                    ) {
                        Text("Refresh", fontFamily = FontFamily.Monospace, color = TerminalWhite)
                    }
                }
            }
        }

        // 3. Telephony & SMS Dispatch
        item {
            HwCard(title = "SMS & TELEPHONY ACTUATOR") {
                OutlinedTextField(
                    value = smsAddress,
                    onValueChange = { smsAddress = it },
                    label = { Text("Phone Number", fontFamily = FontFamily.Monospace) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StatusGreen,
                        unfocusedBorderColor = TerminalBorder,
                        focusedTextColor = TerminalWhite,
                        unfocusedTextColor = TerminalWhite
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = smsMessage,
                    onValueChange = { smsMessage = it },
                    label = { Text("SMS Message Body", fontFamily = FontFamily.Monospace) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StatusGreen,
                        unfocusedBorderColor = TerminalBorder,
                        focusedTextColor = TerminalWhite,
                        unfocusedTextColor = TerminalWhite
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            if (smsAddress.isNotBlank() && smsMessage.isNotBlank()) {
                                val ok = hw.sendSms(smsAddress, smsMessage)
                                Toast.makeText(context, if (ok) "SMS dispatched" else "SMS failed", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusGreen)
                    ) {
                        Text("Send SMS", fontFamily = FontFamily.Monospace, color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            if (smsAddress.isNotBlank()) {
                                hw.dialCall(smsAddress)
                            }
                        },
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(TerminalBorder))
                    ) {
                        Text("Dial", fontFamily = FontFamily.Monospace, color = TerminalWhite)
                    }
                }
            }
        }

        // 4. Haptic Feedback & Vibrations
        item {
            HwCard(title = "HAPTICS & VIBRATION PROFILES") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { hw.vibrate(80) },
                        modifier = Modifier.weight(1f),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(TerminalBorder))
                    ) {
                        Text("Tick", fontFamily = FontFamily.Monospace, color = TerminalWhite)
                    }
                    OutlinedButton(
                        onClick = { hw.vibrate(300) },
                        modifier = Modifier.weight(1f),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(TerminalBorder))
                    ) {
                        Text("Alert", fontFamily = FontFamily.Monospace, color = TerminalWhite)
                    }
                    OutlinedButton(
                        onClick = { hw.vibrate(800) },
                        modifier = Modifier.weight(1f),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(TerminalBorder))
                    ) {
                        Text("SOS", fontFamily = FontFamily.Monospace, color = TerminalWhite)
                    }
                }
            }
        }

        // 5. Connectivity & Location Telemetry
        item {
            HwCard(title = "SENSORS & CONNECTIVITY TELEMETRY") {
                Text("WiFi Telemetry:", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = StatusGreen)
                Text(wifiInfo, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TerminalMuted)
                Spacer(modifier = Modifier.height(6.dp))

                Text("Bluetooth Telemetry:", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = StatusGreen)
                Text(btInfo, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TerminalMuted)
                Spacer(modifier = Modifier.height(6.dp))

                Text("GPS / Geolocation Telemetry:", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = StatusGreen)
                Text(locationInfo, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TerminalMuted)

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        wifiInfo = hw.getWifiStatus().toString(2)
                        btInfo = hw.getBluetoothStatus().toString(2)
                        locationInfo = hw.getLastKnownLocation().toString(2)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF252A30))
                ) {
                    Text("Refresh Telemetry", fontFamily = FontFamily.Monospace, color = TerminalWhite)
                }
            }
        }
    }
}

@Composable
private fun HwCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TerminalSurface),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, TerminalBorder, RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = StatusGreen,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}
