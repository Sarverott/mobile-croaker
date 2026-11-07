package com.example.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages persistent user preferences and system settings across Mobile Croaker.
 * Eliminates hardcoded URLs, ports, and model names.
 */
class AppPreferences private constructor(context: Context) {

    companion object {
        private const val PREFS_NAME = "mobile_croaker_settings"

        @Volatile
        private var instance: AppPreferences? = null

        fun getInstance(context: Context): AppPreferences {
            return instance ?: synchronized(this) {
                instance ?: AppPreferences(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _serverPort = MutableStateFlow(prefs.getInt("server_port", 8080))
    val serverPort: StateFlow<Int> = _serverPort.asStateFlow()

    private val _llmEndpoint = MutableStateFlow(prefs.getString("llm_endpoint", "http://10.0.2.2:11434/v1") ?: "http://10.0.2.2:11434/v1")
    val llmEndpoint: StateFlow<String> = _llmEndpoint.asStateFlow()

    private val _llmModel = MutableStateFlow(prefs.getString("llm_model", "llama3.2:latest") ?: "llama3.2:latest")
    val llmModel: StateFlow<String> = _llmModel.asStateFlow()

    private val _llmTemperature = MutableStateFlow(prefs.getFloat("llm_temperature", 0.2f))
    val llmTemperature: StateFlow<Float> = _llmTemperature.asStateFlow()

    private val _rsyslogHost = MutableStateFlow(prefs.getString("rsyslog_host", "192.168.1.100") ?: "192.168.1.100")
    val rsyslogHost: StateFlow<String> = _rsyslogHost.asStateFlow()

    private val _rsyslogPort = MutableStateFlow(prefs.getInt("rsyslog_port", 514))
    val rsyslogPort: StateFlow<Int> = _rsyslogPort.asStateFlow()

    private val _fastClipboardMode = MutableStateFlow(prefs.getBoolean("fast_clipboard_mode", true))
    val fastClipboardMode: StateFlow<Boolean> = _fastClipboardMode.asStateFlow()

    private val _preferredFallbackTier = MutableStateFlow(prefs.getString("preferred_tier", "TIER_2_TEXT") ?: "TIER_2_TEXT")
    val preferredFallbackTier: StateFlow<String> = _preferredFallbackTier.asStateFlow()

    private val _nodeCallsign = MutableStateFlow(prefs.getString("node_callsign", "CROAKER-ALPHA-01") ?: "CROAKER-ALPHA-01")
    val nodeCallsign: StateFlow<String> = _nodeCallsign.asStateFlow()

    private val _operatorName = MutableStateFlow(prefs.getString("operator_name", "Sett Sarverott / Inceptorium Apokryf") ?: "Sett Sarverott / Inceptorium Apokryf")
    val operatorName: StateFlow<String> = _operatorName.asStateFlow()

    private val _language = MutableStateFlow(prefs.getString("language", "English (en)") ?: "English (en)")
    val language: StateFlow<String> = _language.asStateFlow()

    fun updateSettings(
        port: Int,
        llmUrl: String,
        model: String,
        temp: Float,
        rsyslogHost: String,
        rsyslogPort: Int,
        fastClipboard: Boolean,
        tier: String,
        callsign: String,
        operator: String,
        lang: String
    ) {
        prefs.edit().apply {
            putInt("server_port", port)
            putString("llm_endpoint", llmUrl)
            putString("llm_model", model)
            putFloat("llm_temperature", temp)
            putString("rsyslog_host", rsyslogHost)
            putInt("rsyslog_port", rsyslogPort)
            putBoolean("fast_clipboard_mode", fastClipboard)
            putString("preferred_tier", tier)
            putString("node_callsign", callsign)
            putString("operator_name", operator)
            putString("language", lang)
            apply()
        }

        _serverPort.value = port
        _llmEndpoint.value = llmUrl
        _llmModel.value = model
        _llmTemperature.value = temp
        _rsyslogHost.value = rsyslogHost
        _rsyslogPort.value = rsyslogPort
        _fastClipboardMode.value = fastClipboard
        _preferredFallbackTier.value = tier
        _nodeCallsign.value = callsign
        _operatorName.value = operator
        _language.value = lang
    }

    fun resetToDefaults() {
        updateSettings(
            port = 8080,
            llmUrl = "http://10.0.2.2:11434/v1",
            model = "llama3.2:latest",
            temp = 0.2f,
            rsyslogHost = "192.168.1.100",
            rsyslogPort = 514,
            fastClipboard = true,
            tier = "TIER_2_TEXT",
            callsign = "CROAKER-ALPHA-01",
            operator = "Sett Sarverott / Inceptorium Apokryf",
            lang = "English (en)"
        )
    }
}
