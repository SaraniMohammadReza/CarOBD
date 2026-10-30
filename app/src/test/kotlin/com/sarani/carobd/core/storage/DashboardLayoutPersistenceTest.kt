package com.sarani.carobd.core.storage

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.sarani.carobd.core.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class DashboardLayoutPersistenceTest {
    @Test fun orderStyleSizeAndEmptyLayoutSurviveRecreation() = runBlocking {
        val dir = Files.createTempDirectory("dashboard-layout").toFile()
        val file = dir.resolve("layout.preferences_pb")
        val layout = listOf(DashboardWidgetConfig("second", "maf", WidgetType.OSCILLOSCOPE_GRAPH, WidgetSize.LARGE),
            DashboardWidgetConfig("first", "rpm", WidgetType.DIGITAL_CARD, WidgetSize.MEDIUM))
        suspend fun withStore(block: suspend (AppPreferencesManager) -> Unit) {
            val job = SupervisorJob()
            try { block(AppPreferencesManager(PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + job), produceFile = { file }))) }
            finally { job.cancelAndJoin() }
        }
        try {
            withStore { it.saveDashboardWidgets(layout) }
            withStore { assertEquals(layout, it.dashboardWidgets.first()); it.saveDashboardWidgets(emptyList()) }
            withStore { assertTrue(it.dashboardWidgets.first().isEmpty()) }
        } finally { dir.deleteRecursively() }
    }
    @Test fun malformedOversizedDuplicateAndUnknownEnumLayoutsAreRejected() {
        assertNull(DashboardLayoutCodec.decode("bad"))
        assertNull(DashboardLayoutCodec.decode("id|rpm|NEW_TYPE|MEDIUM|"))
        assertNull(DashboardLayoutCodec.decode("id|rpm|DIGITAL_CARD|MEDIUM|\nid|maf|DIGITAL_CARD|MEDIUM|"))
        assertNull(DashboardLayoutCodec.decode("x".repeat(20_000)))
        assertNull(DashboardLayoutCodec.decode((1..25).joinToString("\n") { "w$it|rpm|DIGITAL_CARD|MEDIUM|" }))
    }
}
