package com.sarani.carobd.core.model

/**
 * User-facing transport selector for the dashboard connection picker.
 *
 * This file intentionally keeps ONLY [ConnectionType]. The authoritative runtime
 * connection lifecycle is [com.sarani.carobd.core.model.ConnectionState] — the 20-state
 * sealed interface defined in the core/model module and driven truthfully by
 * Elm327Adapter (Mode 01 PID 00 must respond before Connected is emitted). Previously a
 * 5-state shadow `ConnectionState` was co-located here in the app module, producing a
 * duplicate-FQCN runtime class and NoSuchFieldError on divergent members; removing it
 * leaves exactly one `ConnectionState` definition (core/model), resolved via the shared
 * `com.sarani.carobd.core.model` package across the app + dependency modules.
 */
enum class ConnectionType {
    VIRTUAL_SIMULATOR,
    BLUETOOTH_CLASSIC,
    BLUETOOTH_LE,
    WIFI_TCP,
    USB_SERIAL
}
