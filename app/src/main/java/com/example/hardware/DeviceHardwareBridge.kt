package com.example.hardware

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentProviderOperation
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.location.Location
import android.location.LocationManager
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.ContactsContract
import android.telephony.SmsManager
import android.util.Log
import com.example.accessibility.MobileCroakerAccessibilityService
import org.json.JSONArray
import org.json.JSONObject

/**
 * Mobile Croaker Hardware & System Integrations Bridge.
 * Exposes device sensors, hardware controls, telephony, and clipboard for autonomous AI agents.
 */
class DeviceHardwareBridge(private val context: Context) {

    companion object {
        private const val TAG = "DeviceHardwareBridge"

        @Volatile
        private var instance: DeviceHardwareBridge? = null

        fun getInstance(context: Context): DeviceHardwareBridge {
            return instance ?: synchronized(this) {
                instance ?: DeviceHardwareBridge(context.applicationContext).also { instance = it }
            }
        }
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val bluetoothAdapter: BluetoothAdapter? = try { BluetoothAdapter.getDefaultAdapter() } catch (_: Throwable) { null }

    private var isTorchActive = false

    // =========================================================================
    // 1. CLIPBOARD MANAGEMENT
    // =========================================================================

    fun getClipboardText(): String {
        return try {
            val clip = clipboardManager?.primaryClip
            if (clip != null && clip.itemCount > 0) {
                clip.getItemAt(0).coerceToText(context).toString()
            } else ""
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read clipboard", e)
            ""
        }
    }

    fun setClipboardText(label: String = "MobileCroaker", text: String): Boolean {
        return try {
            val clip = ClipData.newPlainText(label, text)
            clipboardManager?.setPrimaryClip(clip)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write clipboard", e)
            false
        }
    }

    // =========================================================================
    // 2. BLUETOOTH CONTROL
    // =========================================================================

    @SuppressLint("MissingPermission")
    fun getBluetoothStatus(): JSONObject {
        val json = JSONObject()
        val adapter = bluetoothAdapter
        json.put("available", adapter != null)
        json.put("enabled", adapter?.isEnabled == true)
        json.put("name", adapter?.name ?: "Unknown")

        val bondedList = JSONArray()
        try {
            adapter?.bondedDevices?.forEach { device ->
                val devObj = JSONObject().apply {
                    put("name", device.name ?: "Unnamed")
                    put("address", device.address)
                    put("type", device.type)
                }
                bondedList.put(devObj)
            }
        } catch (_: Exception) {}
        json.put("bondedDevices", bondedList)
        return json
    }

    // =========================================================================
    // 3. WIFI MANAGEMENT
    // =========================================================================

    @SuppressLint("MissingPermission")
    fun getWifiStatus(): JSONObject {
        val json = JSONObject()
        val wifi = wifiManager
        json.put("available", wifi != null)
        json.put("enabled", wifi?.isWifiEnabled == true)

        try {
            val info = wifi?.connectionInfo
            json.put("ssid", info?.ssid ?: "")
            json.put("bssid", info?.bssid ?: "")
            json.put("rssi", info?.rssi ?: 0)
            json.put("linkSpeedMbps", info?.linkSpeed ?: 0)
        } catch (_: Exception) {}
        return json
    }

    // =========================================================================
    // 4. CAMERA & FLASHLIGHT
    // =========================================================================

    fun setFlashlight(enable: Boolean): Boolean {
        val cm = cameraManager ?: return false
        return try {
            val cameraId = cm.cameraIdList.firstOrNull { id ->
                val chars = cm.getCameraCharacteristics(id)
                val flash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val lens = chars.get(CameraCharacteristics.LENS_FACING)
                flash && lens == CameraCharacteristics.LENS_FACING_BACK
            } ?: cm.cameraIdList.firstOrNull() ?: return false

            cm.setTorchMode(cameraId, enable)
            isTorchActive = enable
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle flashlight", e)
            false
        }
    }

    fun isFlashlightOn(): Boolean = isTorchActive

    // =========================================================================
    // 5. AUDIO & VOLUME CONTROL
    // =========================================================================

    fun setSpeakerphone(enable: Boolean): Boolean {
        val am = audioManager ?: return false
        return try {
            am.isSpeakerphoneOn = enable
            true
        } catch (e: Exception) {
            false
        }
    }

    fun adjustVolume(streamType: Int, direction: Int): Boolean {
        val am = audioManager ?: return false
        return try {
            am.adjustStreamVolume(streamType, direction, AudioManager.FLAG_SHOW_UI)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getAudioTelemetry(): JSONObject {
        val am = audioManager ?: return JSONObject()
        return JSONObject().apply {
            put("mode", am.mode)
            put("isSpeakerphoneOn", am.isSpeakerphoneOn)
            put("isMusicActive", am.isMusicActive)
            put("volumeMusic", am.getStreamVolume(AudioManager.STREAM_MUSIC))
            put("maxVolumeMusic", am.getStreamMaxVolume(AudioManager.STREAM_MUSIC))
            put("volumeRing", am.getStreamVolume(AudioManager.STREAM_RING))
            put("volumeVoiceCall", am.getStreamVolume(AudioManager.STREAM_VOICE_CALL))
        }
    }

    // =========================================================================
    // 6. GEOLOCATION SENSORS
    // =========================================================================

    @SuppressLint("MissingPermission")
    fun getLastKnownLocation(): JSONObject {
        val lm = locationManager ?: return JSONObject().put("error", "Location unavailable")
        val json = JSONObject()
        try {
            val providers = lm.getProviders(true)
            var bestLoc: Location? = null
            for (p in providers) {
                val loc = lm.getLastKnownLocation(p) ?: continue
                if (bestLoc == null || loc.accuracy < bestLoc.accuracy) {
                    bestLoc = loc
                }
            }
            if (bestLoc != null) {
                json.put("latitude", bestLoc.latitude)
                json.put("longitude", bestLoc.longitude)
                json.put("accuracyMeters", bestLoc.accuracy)
                json.put("altitude", bestLoc.altitude)
                json.put("provider", bestLoc.provider)
                json.put("timestamp", bestLoc.time)
            } else {
                json.put("status", "No cached GPS location fix")
            }
        } catch (e: Exception) {
            json.put("error", e.message ?: "Permission or GPS disabled")
        }
        return json
    }

    // =========================================================================
    // 7. CONTACTS DIRECTORY
    // =========================================================================

    @SuppressLint("Range")
    fun queryContacts(query: String? = null, limit: Int = 10): JSONArray {
        val array = JSONArray()
        try {
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            val selection = if (!query.isNullOrBlank()) {
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            } else null
            val args = if (!query.isNullOrBlank()) arrayOf("%$query%") else null

            context.contentResolver.query(uri, projection, selection, args, null)?.use { cursor ->
                var count = 0
                while (cursor.moveToNext() && count < limit) {
                    val name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)) ?: ""
                    val number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)) ?: ""
                    array.put(JSONObject().apply {
                        put("name", name)
                        put("number", number)
                    })
                    count++
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Contacts query warning: ${e.message}")
        }
        return array
    }

    fun addContact(name: String, phoneNumber: String): Boolean {
        return try {
            val ops = ArrayList<ContentProviderOperation>()
            val rawContactInsertIndex = ops.size

            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build()
            )

            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                    .build()
            )

            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                    .build()
            )

            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add contact", e)
            false
        }
    }

    // =========================================================================
    // 8. TELEPHONY & SMS
    // =========================================================================

    fun sendSms(address: String, message: String): Boolean {
        return try {
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            smsManager.sendTextMessage(address, null, message, null, null)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS", e)
            false
        }
    }

    fun dialCall(phoneNumber: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start dialer", e)
            false
        }
    }

    // =========================================================================
    // 9. VIBRATION & HAPTICS
    // =========================================================================

    @SuppressLint("MissingPermission")
    fun vibrate(durationMs: Long = 300) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    v?.vibrate(durationMs)
                }
            }
        } catch (_: Exception) {}
    }

    // =========================================================================
    // 10. PHYSICAL KEY SIMULATION & SCREENSHOTS (Via Accessibility)
    // =========================================================================

    fun takeAccessibilityScreenshot(): Boolean {
        val a11y = MobileCroakerAccessibilityService.instance ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            a11y.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
        } else {
            false
        }
    }

    fun pressPowerDialog(): Boolean {
        val a11y = MobileCroakerAccessibilityService.instance ?: return false
        return a11y.performGlobalAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG)
    }

    fun lockScreen(): Boolean {
        val a11y = MobileCroakerAccessibilityService.instance ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            a11y.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
        } else {
            false
        }
    }
}
