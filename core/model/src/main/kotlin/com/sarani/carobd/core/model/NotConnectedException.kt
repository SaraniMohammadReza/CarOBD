package com.sarani.carobd.core.model

/**
 * Signals that a diagnostic operation cannot complete because there is no live
 * ECU connection: either no adapter is connected, or the ECU did not answer a
 * request.
 *
 * This is distinct from "no codes found" — it means a truthful result could not
 * be obtained, so the UI MUST NOT report success, an empty code set, or
 * "ECU Normal". Consumers must surface a NotConnected / Failure state instead.
 */
class NotConnectedException(message: String) : Exception(message)
