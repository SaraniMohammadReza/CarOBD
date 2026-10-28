package com.sarani.carobd.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** §18: manual ELM protocol code → ProtocolStandard mapping. */
class ProtocolStandardMapperTest {

    @Test
    fun autoAndKnownCodesMap() {
        assertEquals(ProtocolStandard.AUTO, protocolFromManualCode("0"))
        assertEquals(ProtocolStandard.SAE_J1850_PWM, protocolFromManualCode("1"))
        assertEquals(ProtocolStandard.ISO_15765_4_CAN_11_500, protocolFromManualCode("6"))
        assertEquals(ProtocolStandard.ISO_15765_4_CAN_29_500, protocolFromManualCode("7"))
        assertEquals(ProtocolStandard.SAE_J1939_CAN, protocolFromManualCode("A"))
    }

    @Test
    fun blankAndUnknownCodesAreNull() {
        assertNull(protocolFromManualCode(""))
        assertNull(protocolFromManualCode("   "))
        // B/C are not modeled — must NOT silently map to AUTO.
        assertNull(protocolFromManualCode("B"))
        assertNull(protocolFromManualCode("C"))
        assertNull(protocolFromManualCode("Z"))
    }

    @Test
    fun lowercaseAndPaddingNormalized() {
        assertEquals(ProtocolStandard.ISO_9141_2, protocolFromManualCode(" 3 "))
        assertEquals(ProtocolStandard.SAE_J1939_CAN, protocolFromManualCode("a"))
    }
}
