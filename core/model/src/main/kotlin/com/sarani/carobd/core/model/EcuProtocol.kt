package com.sarani.carobd.core.model

/**
 * Canonical ECU protocol preset — owned by `core:model` so the app and library
 * modules share exactly one definition.
 *
 * C2 (duplicate-FQCN remediation): previously this data class AND a smaller
 * 5-variant `ProtocolStandard` enum were co-located in
 * `app/src/.../core/model`, while `core/model` declared a 13-variant canonical
 * `ProtocolStandard` (with `isCanBus`, `baudRate`, advanced A/B/C variants).
 * App-module source then resolved the shadow enum during its own compilation
 * (so `adapter.currentProtocol?.isCanBus` failed to compile and had to be
 * worked around via `ObdAdapter.isCanTransport`), while `core:adapter` resolved
 * the canonical enum — a latent divergence masked only by that workaround.
 *
 * The shadow enum is deleted here; this class now references the single
 * canonical [ProtocolStandard] from [ObdProtocol].
 *
 * See also: [com.sarani.carobd.core.model.EcuProtocolConfig] for the lower-level
 * adapter-side protocol configuration (filters / timeouts / confidence).
 */
data class EcuProtocol(
    val id: String,
    val nameEn: String,
    val nameFa: String,
    val standard: ProtocolStandard,
    val targetAddressHex: String? = null, // e.g. "11" or "10" for K-Line ECUs
    val testerAddressHex: String? = "F1",
    val customHeaderCommand: String? = null, // e.g. "AT SH 81 11 F1"
    val wakeUpMessageCommand: String? = null, // e.g. "AT WM 81 11 F1 81"
    val initSequence: List<String> = emptyList(),
    val notesFa: String = ""
)
