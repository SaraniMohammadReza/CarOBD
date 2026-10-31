package com.sarani.carobd.core.di

import com.sarani.carobd.core.connectivity.BluetoothDeviceScanner
import com.sarani.carobd.core.logger.TripLogger
import com.sarani.carobd.core.storage.AppPreferencesManager
import com.sarani.carobd.data.db.CarObdDatabaseHelper
import com.sarani.carobd.data.repository.ActiveVehicleRepository
import com.sarani.carobd.data.repository.ObdRepositoryImpl
import com.sarani.carobd.data.repository.OwnedVehicleRepository
import com.sarani.carobd.data.repository.SQLiteOwnedVehicleRepository
import com.sarani.carobd.domain.repository.ObdRepository
import com.sarani.carobd.domain.usecase.ClearDtcUseCase
import com.sarani.carobd.domain.usecase.ConnectUseCase
import com.sarani.carobd.domain.usecase.DisconnectUseCase
import com.sarani.carobd.domain.usecase.ObserveTelemetryUseCase
import com.sarani.carobd.domain.usecase.ScanDtcUseCase
import com.sarani.carobd.domain.usecase.SendRawCommandUseCase
import com.sarani.carobd.domain.usecase.SetVehicleProfileUseCase
import com.sarani.carobd.ui.screens.connection.ConnectionCenterViewModel
import com.sarani.carobd.ui.screens.dashboard.DashboardViewModel
import com.sarani.carobd.ui.screens.diagnostics.DiagnosticsViewModel
import com.sarani.carobd.ui.screens.maintenance.MaintenanceViewModel
import com.sarani.carobd.ui.screens.owned.OwnedVehiclesViewModel
import com.sarani.carobd.ui.screens.terminal.TerminalViewModel
import com.sarani.carobd.ui.screens.simulator.SimulatorSelectorViewModel
import com.sarani.carobd.ui.screens.trip.TripViewModel
import com.sarani.carobd.ui.screens.vehicles.VehicleProfileViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { com.sarani.carobd.data.recording.RecordingController(com.sarani.carobd.core.adapter.transport.FileTranscriptStore(java.io.File(androidContext().filesDir, "diagnostic-transcripts"))) }
    // Repositories, Loggers, Scanners & Storage
    single { val p = get<com.sarani.carobd.core.storage.AppPreferencesManager>(); ActiveVehicleRepository(p.activeVehicleId, { p.setActiveVehicleId(it) }, { p.clearActiveVehicleId() }) }
    single<ObdRepository> { ObdRepositoryImpl(androidContext(), get(), get(), get(), recordings = get()) }
    single<com.sarani.carobd.core.adapter.profile.ConnectionProfileRepository> {
        com.sarani.carobd.data.repository.DataStoreConnectionProfileRepository(androidContext())
    }
    single { CarObdDatabaseHelper(androidContext()) }
    single<OwnedVehicleRepository> { SQLiteOwnedVehicleRepository(get()) }
    single { BluetoothDeviceScanner(androidContext()) }
    single { TripLogger() }
    single { AppPreferencesManager(androidContext()) }

    // UseCases
    factory { ConnectUseCase(get()) }
    factory { DisconnectUseCase(get()) }
    factory { ObserveTelemetryUseCase(get()) }
    factory { ScanDtcUseCase(get()) }
    factory { ClearDtcUseCase(get()) }
    factory { SendRawCommandUseCase(get()) }
    factory { SetVehicleProfileUseCase(get()) }

    factory { com.sarani.carobd.domain.usecase.ReadinessUseCase(get()) }
    viewModel { com.sarani.carobd.ui.screens.evidence.ReadinessViewModel(get()) }

    factory { com.sarani.carobd.domain.usecase.VehicleInformationUseCase(get()) }
    viewModel { com.sarani.carobd.ui.screens.evidence.VehicleInformationViewModel(get()) }

    factory { com.sarani.carobd.domain.usecase.FreezeFrameUseCase(get()) }
    viewModel { com.sarani.carobd.ui.screens.evidence.FreezeFrameViewModel(get()) }

    viewModel { com.sarani.carobd.ui.screens.recording.RecordingViewModel(get()) }

    viewModel { com.sarani.carobd.ui.screens.evidence.DtcHistoryViewModel(get()) }

    factory { com.sarani.carobd.domain.usecase.MonitorTestsUseCase(get()) }
    viewModel { com.sarani.carobd.ui.screens.evidence.MonitorTestsViewModel(get()) }

    // ViewModels
    viewModel { ConnectionCenterViewModel(get(), get()) }
    viewModel { DashboardViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { DiagnosticsViewModel(get(), get()) }
    viewModel { VehicleProfileViewModel(get(), get(), get()) }
    viewModel { TerminalViewModel(get()) }
    viewModel { SimulatorSelectorViewModel(get(), get()) }
    viewModel { MaintenanceViewModel(get(), get()) }
    viewModel { OwnedVehiclesViewModel(get()) }
    viewModel { TripViewModel(get(), get(), get()) }
}
