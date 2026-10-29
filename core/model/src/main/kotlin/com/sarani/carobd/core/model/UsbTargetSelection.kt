package com.sarani.carobd.core.model

data class UsbDeviceIdentity(val deviceId: Int, val vendorId: Int, val productId: Int, val interfaceCount: Int)
object UsbTargetSelection {
    fun select(config: TransportConfig.Usb, available: List<UsbDeviceIdentity>): UsbDeviceIdentity {
        require(config.deviceId != null || (config.vendorId != null && config.productId != null)) {
            "USB_CONFIGURATION_REQUIRED: choose device identity and interface"
        }
        val port = requireNotNull(config.portIndex) { "USB_CONFIGURATION_REQUIRED: choose interface/port" }
        val matches = available.filter { d ->
            (config.deviceId == null || d.deviceId == config.deviceId) &&
            (config.vendorId == null || d.vendorId == config.vendorId) &&
            (config.productId == null || d.productId == config.productId)
        }
        require(matches.size == 1) { "DEVICE_UNAVAILABLE: selected USB device absent or ambiguous" }
        return matches.single().also { require(port in 0 until it.interfaceCount) { "DEVICE_UNAVAILABLE: selected interface absent" } }
    }
}
