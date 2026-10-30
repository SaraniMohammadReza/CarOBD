package com.sarani.carobd

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.xmlpull.v1.XmlPullParser

/** Verifies shipped policy resources. Does not invoke a cloud backup or claim a device migration. */
class BackupPrivacyInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private data class Rule(val mode: String, val tag: String, val domain: String?, val path: String?)
    private fun rules(resource: Int): List<Rule> = buildList {
        context.resources.getXml(resource).use { xml ->
            var mode = "legacy"
            while (xml.eventType != XmlPullParser.END_DOCUMENT) {
                if (xml.eventType == XmlPullParser.START_TAG) {
                    if (xml.name in setOf("cloud-backup", "device-transfer")) mode = xml.name
                    if (xml.name in setOf("exclude", "include")) add(Rule(mode, xml.name,
                        xml.getAttributeValue(null, "domain"), xml.getAttributeValue(null, "path")))
                }
                xml.next()
            }
        }
    }

    @Test fun privateEvidenceExcludedFromCloudWhileDeviceTransferDoesNotRestorePermissionHistory() {
        val domains = setOf("root", "file", "database", "sharedpref", "external", "device_root",
            "device_file", "device_database", "device_sharedpref")
        val legacy = rules(R.xml.backup_rules)
        val modern = rules(R.xml.data_extraction_rules)
        for (policy in listOf(legacy, modern.filter { it.mode == "cloud-backup" })) {
            assertEquals(domains, policy.filter { it.tag == "exclude" && it.path == "." }.map { it.domain }.toSet())
            assertFalse(policy.any { it.tag == "include" })
        }
        // Database reports, files/datastore preferences and recorded transcripts all fall under these roots.
        val transfer = modern.filter { it.mode == "device-transfer" }
        assertTrue(transfer.contains(Rule("device-transfer", "exclude", "sharedpref", "runtime_access.xml")))
        assertFalse("Owner database must remain eligible for explicit device transfer", transfer.any { it.domain == "database" })
    }
}
