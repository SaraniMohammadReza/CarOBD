package com.sarani.carobd.core.model

/** Software results and physical validation are deliberately separate states. */
enum class ValidationEvidenceState { SOFTWARE_TESTED, ANDROID_BUILD_VERIFIED, DEVICE_NOT_TESTED, DEVICE_PASS, VEHICLE_NOT_TESTED, VEHICLE_PASS }
enum class BenchPhase { PermissionSnapshot, PermissionRequested, PermissionGranted, PermissionDenied,
    TransportConnecting, TransportConnected, AdapterInitializing, AdapterResponding, ProtocolSelecting,
    ProtocolObserved, EcuHandshake, EcuResponding, CapabilitiesDiscovered, TransportLost, DisconnectRequested, SessionEnded, ConnectionFailed }
