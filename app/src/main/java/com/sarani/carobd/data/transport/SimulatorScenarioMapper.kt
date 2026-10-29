package com.sarani.carobd.data.transport

/** Missing legacy scenario uses the explicit simulator default; unknown IDs fail. */
fun simulatorScenarioFromId(id: String?): SimulatorScenario {
    if (id.isNullOrBlank()) return SimulatorScenario.HEALTHY_VEHICLE
    return SimulatorScenario.entries.find { it.id == id.trim().lowercase() }
        ?: throw IllegalArgumentException("Unknown simulator scenario: $id")
}
