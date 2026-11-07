package com.example

import com.example.agent.YamlScenarioManager
import com.example.identity.IdentityFormat
import com.example.identity.PhoneIdentity
import com.example.identity.PhoneIdentityManager
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
    fun `yaml scenario manager parses and serializes automation workflows`() {
        val yaml = """
            version: "1.0"
            id: "test-flow"
            title: "Test Unit Scenario"
            target_package: "com.android.settings"
            steps:
              - name: "Launch Settings"
                action: "LAUNCH_APP"
                delay_ms: 500
              - name: "Tap Battery"
                action: "CLICK_NODE"
                target_text: "Battery"
                coords: [540, 220]
        """.trimIndent()

        val parsed = YamlScenarioManager.fromYaml(yaml)
        assertTrue("YAML parsing should succeed", parsed.isSuccess)
        val wf = parsed.getOrThrow()
        assertEquals("test-flow", wf.id)
        assertEquals("Test Unit Scenario", wf.title)
        assertEquals(2, wf.steps.size)
        assertEquals("Launch Settings", wf.steps[0].name)
        assertEquals("Battery", wf.steps[1].targetText)

        val serialized = YamlScenarioManager.toYaml(wf)
        assertTrue(serialized.contains("title: \"Test Unit Scenario\""))
        assertTrue(serialized.contains("target_package: \"com.android.settings\""))
    }

    @Test
    fun `phone identity manager exports across markdown yaml json toml csv`() {
        val identity = PhoneIdentity(
            callsign = "RAUSON-Merlin-Unit",
            operator = "Inceptorium Apokryf (apokryf.pl)"
        )

        val md = PhoneIdentityManager.export(identity, IdentityFormat.MARKDOWN)
        val yaml = PhoneIdentityManager.export(identity, IdentityFormat.YAML)
        val json = PhoneIdentityManager.export(identity, IdentityFormat.JSON)
        val toml = PhoneIdentityManager.export(identity, IdentityFormat.TOML)
        val csv = PhoneIdentityManager.export(identity, IdentityFormat.CSV)

        assertTrue(md.contains("RAUSON-Merlin-Unit"))
        assertTrue(yaml.contains("callsign: \"RAUSON-Merlin-Unit\""))
        assertTrue(json.contains("\"callsign\": \"RAUSON-Merlin-Unit\""))
        assertTrue(toml.contains("callsign = \"RAUSON-Merlin-Unit\""))
        assertTrue(csv.contains("callsign,\"RAUSON-Merlin-Unit\""))
    }

    @Test
    fun `verify syslog severities`() {
        val emergency = SyslogSeverity.EMERGENCY
        val info = SyslogSeverity.INFORMATIONAL
        assertEquals(0, emergency.code)
        assertEquals(6, info.code)
    }
}
