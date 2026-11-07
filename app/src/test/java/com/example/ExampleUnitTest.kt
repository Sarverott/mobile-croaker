package com.example

import com.example.model.ExecutionTier
import com.example.model.SyslogSeverity
import com.example.telemetry.RsyslogClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `test execution tiers cover complete 4-tier fallback stack`() {
        val tiers = ExecutionTier.values()
        assertEquals(4, tiers.size)
        assertTrue(tiers.contains(ExecutionTier.ACCESSIBILITY_NODE))
        assertTrue(tiers.contains(ExecutionTier.ACCESSIBILITY_GESTURE))
        assertTrue(tiers.contains(ExecutionTier.ADB_BRIDGE))
        assertTrue(tiers.contains(ExecutionTier.CV_HEURISTIC))
    }

    @Test
    fun `rsyslog telemetry client buffers and streams audit entries`() {
        val client = RsyslogClient.instance
        client.log(SyslogSeverity.INFORMATIONAL, "UNIT_TEST", "Validation of encrypted telemetry channel.")
        val entries = client.logs.value
        assertTrue("Telemetry buffer must contain entries", entries.isNotEmpty())
        assertEquals("UNIT_TEST", entries.first().appName)
        assertTrue(entries.first().isTlsEncrypted)
    }

    @Test
    fun `verify syslog severities`() {
        val emergency = SyslogSeverity.EMERGENCY
        val info = SyslogSeverity.INFORMATIONAL
        assertEquals(0, emergency.code)
        assertEquals(6, info.code)
    }
}
