package com.sarani.carobd.data.transport

/**
 * Deterministic synthetic scenarios for [IsolatedSimulatorTransport], mapped
 * directly to the §39 error/edge-case catalogue. Selected explicitly so the
 * simulator is never mistaken for a physical ECU (§11).
 */
enum class SimulatorScenario {
    /** Default healthy vehicle with running engine, codes stored/clearable. */
    HEALTHY_VEHICLE,

    /** ECU powers off / silent bus — no response frames are emitted (timeout path). */
    ENGINE_OFF,

    /** ECU is silent; commands elicit no response, triggering adapter timeout. */
    TIMEOUT,

    /** ECU responds to every OBD request with NO DATA. */
    NO_DATA,

    /** Bus initialisation fails (K-line / CAN init). */
    BUS_INIT_ERROR,

    /** CAN bus error reported by the adapter. */
    CAN_ERROR,

    /** ECU responds but rejects requests (7F negative response). */
    NEGATIVE_RESPONSE,

    /** One or more stored DTCs present. */
    STORED_DTC,

    /** One or more pending DTCs present. */
    PENDING_DTC,

    /** Healthy vehicle, but the ECU prompt (">") is delayed. */
    LATE_PROMPT,
}
