package com.sarani.carobd.core.storage

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.sarani.carobd.core.adapter.profile.InMemoryConnectionProfileRepository
import com.sarani.carobd.core.model.TelemetryPollMode
import com.sarani.carobd.data.repository.ActiveVehicleRepository
import com.sarani.carobd.data.repository.ObdRepositoryImpl
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class PollModePersistenceTest {
    @Test fun savedModeSurvivesRecreationAndLoadsWithoutRepositoryObserver() = runBlocking {
        val directory = Files.createTempDirectory("carobd-poll").toFile()
        val file = directory.resolve("poll.preferences_pb")
        val firstJob = SupervisorJob()
        val firstStore = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + firstJob), produceFile = { file })
        AppPreferencesManager(firstStore).setTelemetryPollMode(TelemetryPollMode.ECO)
        firstJob.cancelAndJoin()
        val secondJob = SupervisorJob()
        try {
            val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + secondJob), produceFile = { file })
            val prefs = AppPreferencesManager(store)
            assertEquals(TelemetryPollMode.ECO, prefs.telemetryPollMode.first())
            val observed = CompletableDeferred<Unit>()
            val repo = ObdRepositoryImpl(null, ActiveVehicleRepository(MutableStateFlow(null), {}, {}), prefs,
                InMemoryConnectionProfileRepository(), scope = CoroutineScope(Dispatchers.IO + secondJob),
                pollModeFlow = prefs.telemetryPollMode.onEach { observed.complete(Unit) }, startService = {}, stopService = {})
            // No collector attaches to pollModeState. WhileSubscribed would time out.
            withTimeout(5000) { observed.await(); while (repo.pollModeState.value != TelemetryPollMode.ECO) yield() }
            prefs.setTelemetryPollMode(TelemetryPollMode.FAST)
            withTimeout(5000) { while (repo.pollModeState.value != TelemetryPollMode.FAST) yield() }
        } finally { secondJob.cancelAndJoin(); directory.deleteRecursively() }
    }
}
