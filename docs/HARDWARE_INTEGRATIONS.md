# Mobile Croaker Hardware & Subsystem Integrations

Mobile Croaker provides deep hardware integration on physical Android devices. This document explains how the hardware layer is designed, how permissions are managed, and how external agents interact with on-device hardware.

---

## 1. Subsystem Overview

The hardware subsystem is exposed via `com.example.hardware.DeviceHardwareBridge`:

```
┌────────────────────────────────────────────────────────┐
│           DeviceHardwareBridge (Singleton)             │
└───────────────────────────┬────────────────────────────┘
                            │
   ┌───────────────┬────────┼────────┬──────────────┐
   ▼               ▼        ▼        ▼              ▼
Clipboard       Camera    Audio   Sensors/       Telephony
Manager         Manager   Manager Location       & SMS
(Zero-Keyb)     (Torch)   (Streams & (GPS /      (SmsManager &
                          Mic)    Network)       Telecom)
```

---

## 2. Hardware Capabilities & APIs

### 2.1 Clipboard Management (Typing Accelerator)
- **Problem**: Virtual keyboards on Android often introduce auto-correct errors, slow software typing delays, and focus drops.
- **Solution**: `DeviceHardwareBridge.setClipboardText(text)` places strings directly on the system clipboard. The Accessibility Service then triggers `AccessibilityNodeInfo.ACTION_PASTE`, enabling instantaneous multi-line text injection.
- **API**:
  - `GET /api/v1/hardware/clipboard`
  - `POST /api/v1/hardware/clipboard` (body: `{"text": "my text"}`)

### 2.2 Camera & Optical Flashlight (Torch)
- **Uses**: Optical diagnostics, low-light inspection, strobe signaling.
- **Implementation**: Utilizes `CameraManager.setTorchMode(cameraId, enabled)`.
- **API**:
  - `POST /api/v1/hardware/torch` (body: `{"enabled": true}`)

### 2.3 Audio & Volume Controls
- **Audio Routing**: Controls `STREAM_RING`, `STREAM_MUSIC`, and `STREAM_ALARM`.
- **Implementation**: `AudioManager.setStreamVolume(...)` with fallback flags to avoid interrupting active system services.
- **API**:
  - `POST /api/v1/hardware/volume` (body: `{"stream": "music", "level": 10}`)

### 2.4 Geolocation (GPS & Network)
- **Location Resolution**: Inspects `LocationManager.GPS_PROVIDER` and `NETWORK_PROVIDER` to fetch the last known coordinates with timestamps, accuracy, and provider tags.
- **API**:
  - `GET /api/v1/hardware/location`
  - Returns: `{"latitude": 52.2297, "longitude": 21.0122, "accuracy": 15.0, "provider": "gps"}`

### 2.5 Telephony, SMS & Contacts
- **SMS Transmission**: Sends direct SMS messages via `SmsManager` (supporting Android 12+ multi-SIM and legacy fallbacks).
- **Phone Calls**: Prepares dialer intents or initiates direct emergency/diagnostic dialing.
- **Contacts**: Reads and adds contacts through `ContactsContract.Data` and `RawContacts`.
- **API**:
  - `POST /api/v1/telephony/sms` (body: `{"phone_number": "+1234567890", "message": "Alert"}`)
  - `POST /api/v1/telephony/dial` (body: `{"phone_number": "+1234567890"}`)
  - `GET /api/v1/telephony/contacts`

### 2.6 Physical Buttons & Navigation Keys
- **Hardware Keys**:
  - `VOLUME_UP` / `VOLUME_DOWN`: Triggers volume step changes.
  - `TAKE_SCREENSHOT`: Uses Accessibility Service global action `GLOBAL_ACTION_TAKE_SCREENSHOT` (Android 9+).
  - `LOCK_SCREEN`: Locks device screen via `GLOBAL_ACTION_LOCK_SCREEN` (Android 9+).
  - `POWER_DIALOG`: Pops the system power menu via `GLOBAL_ACTION_POWER_DIALOG`.
- **API**:
  - `POST /api/v1/hardware/button` (body: `{"button": "TAKE_SCREENSHOT"}`)

---

## 3. Security & Permission Sandboxing

1. **Explicit Consent**: Sensitive permissions are declared in `AndroidManifest.xml` and requested at runtime.
2. **Local Isolation**: Hardware control APIs default to listening on `127.0.0.1` and local WiFi network interfaces only.
3. **Audit Logging**: Every hardware action is logged through the internal syslog / rsyslog telemetry engine.
