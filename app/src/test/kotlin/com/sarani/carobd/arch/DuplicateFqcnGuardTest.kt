package com.sarani.carobd.arch

import com.sarani.carobd.core.model.ProtocolStandard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * C2 regression guard against the duplicate-FQCN failure class.
 *
 * `com.sarani.carobd.core.model.ProtocolStandard` must resolve to the CANONICAL
 * definition in `core/model` (13 variants, exposing `isCanBus` / `baudRate` /
 * advanced A/B/C variants), and must NOT be an app-module-local shadow enum.
 *
 * If anyone re-declares `ProtocolStandard` inside `app/src`, the app-module
 * classpath would resolve the shadow (5 variants, no `isCanBus`) and this test
 * would fail — surfacing the duplicate-FQCN bug *before* it silently regresses
 * compilation or runtime semantics (cf. the earlier duplicate-Model/$stable
 * crash class and this run's duplicate-ProtocolStandard finding).
 *
 * Combined with `..:checkDebugDuplicateClasses` in the CI gate, this makes the
 * single-authoritative-core-model invariant mechanically verifiable.
 */
class DuplicateFqcnGuardTest {

    @Test
    fun protocolStandardResolvesToCanonicalNotShadow() {
        val values = ProtocolStandard.entries
        // core/model canonical defines 15 variants (incl. advanced A/B/C and
        // 2nd-gen UDS/ZEV); the deleted app shadow had only 5.
        assertEquals(15, values.size)
        // Canonical exposes advanced protocols the shadow omitted.
        assertTrue(values.any { it.name == "ISO_14229_UDS_CAN" })
        assertTrue(values.any { it.name == "ISO_27145_WWH_OBD" })
        assertTrue(values.any { it.name.startsWith("SAE_J1979") })
        // `isCanBus` is a canonical constructor member; the shadow lacked it.
        val can = values.find { it.name == "ISO_15765_4_CAN_11_500" }!!
        assertTrue(can.isCanBus)
        assertFalse(ProtocolStandard.AUTO.isCanBus)
        assertFalse(ProtocolStandard.SAE_J1850_PWM.isCanBus)
    }
}
