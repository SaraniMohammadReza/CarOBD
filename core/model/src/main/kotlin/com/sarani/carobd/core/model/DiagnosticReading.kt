package com.sarani.carobd.core.model

/** Outcomes remain per responder, including failed peers beside a successful ECU. */
enum class ReadingState { Success, NoData, NegativeResponse, Timeout, Malformed, Partial, Unsupported, Disconnected, AdapterFailure }
data class DiagnosticReading<T>(val state: ReadingState, val value: T? = null, val evidence: List<DiagnosticWireResponse> = emptyList(), val detail: String? = null)
enum class IgnitionType { Spark, Compression }
