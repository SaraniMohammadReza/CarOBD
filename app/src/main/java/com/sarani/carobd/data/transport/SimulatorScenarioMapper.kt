package com.sarani.carobd.data.transport

/**
 * §17: resolve a persisted scenario id to [SimulatorScenario].
 *
 * Null/blank/unknown → [SimulatorScenario.HEALTHY_VEHICLE] (documented default),
 * so a missing/invalid scenario never silently becomes a physical ECU and never
 * silently re-routes to a non-simulator transport (§14).
 */
fun simulatorScenarioFromId(id: String?): SimulatorScenario =
    id?.trim()?.lowercase()?.let { SimulatorScenario.values().find { s -> s.id == it } }
        ?: SimulatorScenario.HEALTHY_VEHICLE
