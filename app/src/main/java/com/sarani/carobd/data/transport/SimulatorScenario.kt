package com.sarani.carobd.data.transport

/**
 * Deterministic synthetic scenarios for [IsolatedSimulatorTransport], mapped
 * directly to the §39 error/edge-case catalogue. Selected explicitly so the
 * simulator is never mistaken for a physical ECU (§11).
 */
enum class SimulatorScenario(
    val id: String,
    val labelEn: String,
    val labelFa: String,
) {
    HEALTHY_VEHICLE("healthy", "Steady cruise · no simulated DTCs", "حرکت یکنواخت · بدون کد خطای شبیه‌سازی‌شده"),

    ENGINE_STOPPED("engine_stopped", "Engine stopped · ECU responding", "موتور خاموش · ECU پاسخ‌گو"),
    ENGINE_IDLE("engine_idle", "Engine idling · stationary", "موتور درجا · خودرو متوقف"),

    /** ECU powers off / silent bus — no response frames are emitted (timeout path). */
    ENGINE_OFF("engine_off", "ECU powered off · silent", "ECU خاموش · بدون پاسخ"),

    /** ECU is silent; commands elicit no response, triggering adapter timeout. */
    TIMEOUT("timeout", "Timeout (Silent)", "تایم‌آوت (بی‌صدا)"),

    /** ECU responds to every OBD request with NO DATA. */
    NO_DATA("no_data", "NO DATA", "NO DATA"),

    /** Bus initialisation fails (K-line / CAN init). */
    BUS_INIT_ERROR("bus_init_error", "BUS INIT ERROR", "خطای راه‌اندازی شبکه"),

    /** CAN bus error reported by the adapter. */
    CAN_ERROR("can_error", "CAN ERROR", "خطای CAN"),

    /** ECU responds but rejects requests (7F negative response). */
    NEGATIVE_RESPONSE("7f_rejection", "7F Rejection", "رد 7F"),

    /** One or more stored DTCs present. */
    STORED_DTC("stored_dtc", "Stored DTC", "کد خطای ذخیره‌شده"),

    /** One or more pending DTCs present. */
    PENDING_DTC("pending_dtc", "Pending DTC", "کد خطای معلق"),

    PERMANENT_DTC("permanent_dtc", "Permanent DTC", "کد خطای دائمی"),
    MALFORMED("malformed", "Malformed DTC response", "پاسخ نامعتبر خطا"),
    DISCONNECT("disconnect", "Disconnect during RPM read", "قطع ارتباط هنگام خواندن"),
    MULTI_ECU("multi_ecu", "Multi ECU with negative responder", "چند ECU با پاسخ منفی"),

    /** Healthy vehicle, but the ECU prompt (">") is delayed. */
    LATE_PROMPT("late_prompt", "Late Prompt", "علامت تأخیری"),
    /** Adapter reports BUFFER FULL; subsequent writes rejected until reset (§39). */
    BUFFER_FULL("buffer_full", "Buffer Full", "بافر پر"),
}
