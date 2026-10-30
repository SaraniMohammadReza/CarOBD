package com.sarani.carobd.core.model

import org.junit.Assert.*
import org.junit.Test

class DtcHistoryTest {
    @Test fun categoriesRespondersAndUnknownMilArePreserved() {
        val records = DtcCategory.entries.map { category ->
            DtcHistoryCode.capture(DiagnosticCode("P0133", "", "", "", "", category = category, responderAddressHex = "7E9"), null, null, 42)
        }
        assertEquals(DtcCategory.entries.toSet(), records.map { it.category }.toSet())
        assertTrue(records.all { it.mil == null && it.protocol == null && it.responder == "7E9" && it.observedAtMs == 42L })
    }
}
