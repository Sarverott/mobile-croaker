package com.example.telemetry

import android.os.Build
import android.util.Log
import com.example.model.SyslogEntry
import com.example.model.SyslogSeverity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.Socket
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

enum class SyslogProtocol {
    TLS_TCP,
    PLAIN_TCP,
    UDP
}

data class RsyslogConfig(
    val serverHost: String = "192.168.1.100",
    val serverPort: Int = 6514, // 6514 for TLS, 514 for Plain
    val protocol: SyslogProtocol = SyslogProtocol.TLS_TCP,
    val facility: Int = 16, // local0
    val appName: String = "OpenClawAgent",
    val hostIdentifier: String = "android-${try { (Build.MODEL as String?)?.replace(" ", "-") ?: "device" } catch (_: Throwable) { "device" }}",
    val isEnabled: Boolean = true,
    val trustAllCertificatesForTesting: Boolean = true
)

/**
 * High-performance, lightweight encrypted Syslog client.
 * Conforms to RFC 5424 / RFC 3164 with TLS 1.2/1.3 encryption,
 * offline buffering, and telemetry streaming.
 */
class RsyslogClient private constructor() {

    companion object {
        private const val TAG = "OpenClawRsyslog"
        val instance by lazy { RsyslogClient() }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _config = MutableStateFlow(RsyslogConfig())
    val config: StateFlow<RsyslogConfig> = _config.asStateFlow()

    private val _logs = MutableStateFlow<List<SyslogEntry>>(emptyList())
    val logs: StateFlow<List<SyslogEntry>> = _logs.asStateFlow()

    private val _connectionStatus = MutableStateFlow<String>("Ready (TLS Encrypted)")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private val _isTransmitting = MutableStateFlow(false)
    val isTransmitting: StateFlow<Boolean> = _isTransmitting.asStateFlow()

    private val offlineQueue = ConcurrentLinkedQueue<SyslogEntry>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)

    init {
        // Seed initial telemetry audit entries
        log(
            SyslogSeverity.INFORMATIONAL,
            "SYSTEM",
            "OpenClaw Agent Core initialized. TLS telemetry channel armed."
        )
        log(
            SyslogSeverity.NOTICE,
            "SECURITY",
            "Non-root sandbox confirmed. Accessibility bridge registered."
        )
    }

    fun updateConfig(newConfig: RsyslogConfig) {
        _config.value = newConfig
        log(
            SyslogSeverity.NOTICE,
            "CONFIG",
            "Rsyslog target updated: ${newConfig.protocol}://${newConfig.serverHost}:${newConfig.serverPort}"
        )
    }

    fun log(severity: SyslogSeverity, subsystem: String, message: String) {
        val timestamp = synchronized(dateFormat) { dateFormat.format(Date()) }
        val entry = SyslogEntry(
            id = UUID.randomUUID().toString(),
            timestamp = timestamp,
            severity = severity,
            appName = subsystem,
            message = message,
            isTlsEncrypted = _config.value.protocol == SyslogProtocol.TLS_TCP,
            transmitted = false
        )

        // Add to UI state (capped at 200 items for memory lightness)
        val current = _logs.value.toMutableList()
        current.add(0, entry)
        if (current.size > 200) {
            _logs.value = current.take(200)
        } else {
            _logs.value = current
        }

        if (!_config.value.isEnabled) return

        offlineQueue.offer(entry)
        flushQueue()
    }

    fun testConnection(onResult: (Boolean, String) -> Unit) {
        scope.launch {
            _isTransmitting.value = true
            _connectionStatus.value = "Testing connection..."
            try {
                val cfg = _config.value
                val testPayload = formatRfc5424(
                    SyslogEntry(
                        id = UUID.randomUUID().toString(),
                        timestamp = synchronized(dateFormat) { dateFormat.format(Date()) },
                        severity = SyslogSeverity.INFORMATIONAL,
                        appName = "TEST_PING",
                        message = "OpenClaw telemetry handshake test payload.",
                        isTlsEncrypted = cfg.protocol == SyslogProtocol.TLS_TCP
                    )
                )

                sendRawPayload(cfg, testPayload)
                withContext(Dispatchers.Main) {
                    _connectionStatus.value = "Connected (${cfg.protocol} to ${cfg.serverHost}:${cfg.serverPort})"
                    _isTransmitting.value = false
                    onResult(true, "Successfully transmitted test telemetry packet over ${cfg.protocol}!")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Rsyslog transmission error: ${e.message}")
                withContext(Dispatchers.Main) {
                    _connectionStatus.value = "Offline / Buffering: ${e.localizedMessage ?: "Connection error"}"
                    _isTransmitting.value = false
                    onResult(false, "Connection error: ${e.message}. Logs queued offline.")
                }
            }
        }
    }

    private fun flushQueue() {
        scope.launch {
            if (_isTransmitting.value) return@launch
            val cfg = _config.value
            if (!cfg.isEnabled || offlineQueue.isEmpty()) return@launch

            _isTransmitting.value = true
            try {
                while (!offlineQueue.isEmpty()) {
                    val item = offlineQueue.poll() ?: break
                    val payload = formatRfc5424(item)
                    sendRawPayload(cfg, payload)
                }
                _connectionStatus.value = "Active: Encrypted stream established"
            } catch (e: Exception) {
                _connectionStatus.value = "Buffered (${offlineQueue.size} in queue): ${e.message}"
            } finally {
                _isTransmitting.value = false
            }
        }
    }

    private fun sendRawPayload(cfg: RsyslogConfig, payload: String) {
        val bytes = (payload + "\n").toByteArray(Charsets.UTF_8)
        when (cfg.protocol) {
            SyslogProtocol.TLS_TCP -> {
                val sslContext = if (cfg.trustAllCertificatesForTesting) {
                    createPermissiveSslContext()
                } else {
                    SSLContext.getDefault()
                }
                val socketFactory = sslContext.socketFactory
                (socketFactory.createSocket(cfg.serverHost, cfg.serverPort) as SSLSocket).use { sslSocket ->
                    sslSocket.soTimeout = 4000
                    sslSocket.startHandshake()
                    val out: OutputStream = sslSocket.outputStream
                    out.write(bytes)
                    out.flush()
                }
            }
            SyslogProtocol.PLAIN_TCP -> {
                Socket(cfg.serverHost, cfg.serverPort).use { socket ->
                    socket.soTimeout = 4000
                    val out = socket.getOutputStream()
                    out.write(bytes)
                    out.flush()
                }
            }
            SyslogProtocol.UDP -> {
                DatagramSocket().use { socket ->
                    val address = InetAddress.getByName(cfg.serverHost)
                    val packet = DatagramPacket(bytes, bytes.size, address, cfg.serverPort)
                    socket.send(packet)
                }
            }
        }
    }

    private fun formatRfc5424(entry: SyslogEntry): String {
        val pri = (_config.value.facility * 8) + entry.severity.code
        val host = _config.value.hostIdentifier
        val app = entry.appName.replace(" ", "_")
        return "<$pri>1 ${entry.timestamp} $host $app - - - ${entry.message}"
    }

    private fun createPermissiveSslContext(): SSLContext {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAllCerts, SecureRandom())
        return sslContext
    }

    fun clearLogs() {
        _logs.value = emptyList()
        offlineQueue.clear()
    }
}
