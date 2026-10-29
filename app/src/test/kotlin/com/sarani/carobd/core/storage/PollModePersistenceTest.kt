package com.sarani.carobd.core.storage

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.sarani.carobd.core.adapter.profile.InMemoryConnectionProfileRepository
import com.sarani.carobd.core.model.TelemetryPollMode
import com.sarani.carobd.core.telemetry.scheduler.TelemetryProducer
import kotlinx.coroutines.test.*
import com.sarani.carobd.data.repository.ActiveVehicleRepository
import com.sarani.carobd.data.repository.ObdRepositoryImpl
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
class PollModePersistenceTest {
    @Test fun savedEcoSurvivesRecreationAndLoadsWithoutRepositoryObserver() = verifySavedMode(TelemetryPollMode.ECO)
    @Test fun savedFastSurvivesRecreationAndLoadsWithoutRepositoryObserver() = verifySavedMode(TelemetryPollMode.FAST)

    private fun verifySavedMode(savedMode: TelemetryPollMode) = runBlocking {
        val directory = Files.createTempDirectory("carobd-poll").toFile()
        val file = directory.resolve("poll.preferences_pb")
        val firstJob = SupervisorJob()
        val firstStore = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + firstJob), produceFile = { file })
        AppPreferencesManager(firstStore).setTelemetryPollMode(savedMode)
        firstJob.cancelAndJoin()
        val secondJob = SupervisorJob()
        try {
            val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + secondJob), produceFile = { file })
            val prefs = AppPreferencesManager(store)
            assertEquals(savedMode, prefs.telemetryPollMode.first())
            val observed = CompletableDeferred<Unit>()
            val repo = ObdRepositoryImpl(null, ActiveVehicleRepository(MutableStateFlow(null), {}, {}), prefs,
                InMemoryConnectionProfileRepository(), scope = CoroutineScope(Dispatchers.IO + secondJob),
                pollModeFlow = prefs.telemetryPollMode.onEach { observed.complete(Unit) }, startService = {}, stopService = {})
            // No collector attaches to pollModeState. WhileSubscribed would time out.
            withTimeout(5000) { observed.await(); while (repo.pollModeState.value != savedMode) yield() }
            // Feed the production repository state directly to the real producer;
            // no UI collector is needed to activate preference loading.
            runTest {
                val samples = mutableListOf<Long>()
                val producer = TelemetryProducer(repo.pollModeState, { command, _ ->
                    if (command == "0100") "41 00 00 00 00 00" else null
                }, { true }, onTelemetry = { samples.add(testScheduler.currentTime) })
                try {
                    producer.startIn(backgroundScope)
                    runCurrent()
                    advanceTimeBy(savedMode.delayMs - 1); runCurrent()
                    assertEquals(listOf(0L), samples)
                    advanceTimeBy(1); runCurrent()
                    assertEquals(listOf(0L, savedMode.delayMs), samples)
                } finally { producer.stop() }
            }
            val nextMode = if (savedMode == TelemetryPollMode.FAST) TelemetryPollMode.ECO else TelemetryPollMode.FAST
            prefs.setTelemetryPollMode(nextMode)
            withTimeout(5000) { while (repo.pollModeState.value != nextMode) yield() }
        } finally { secondJob.cancelAndJoin(); directory.deleteRecursively() }
    }
}
