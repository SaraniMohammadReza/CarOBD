package com.sarani.carobd.core.model

data class VehicleProfile(
    val id: String,
    val manufacturer: String, // IKCO, SAIPA, Global
    val modelNameEn: String,
    val modelNameFa: String,
    val engineName: String,
    val ecuName: String,
    val ecuProtocol: EcuProtocol,
    val isDefault: Boolean = false
)
