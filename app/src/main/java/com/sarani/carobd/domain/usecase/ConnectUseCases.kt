package com.sarani.carobd.domain.usecase

import com.sarani.carobd.core.model.ConnectionType
import com.sarani.carobd.domain.repository.ObdRepository

class ConnectUseCase(private val repository: ObdRepository) {
    suspend operator fun invoke(type: ConnectionType, target: String = ""): Result<Unit> {
        return repository.connect(type, target)
    }
}

class DisconnectUseCase(private val repository: ObdRepository) {
    suspend operator fun invoke() {
        repository.disconnect()
    }
}
