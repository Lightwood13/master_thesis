package org.kry.thesis.service

import org.kry.thesis.domain.Heater
import org.kry.thesis.repository.HeaterRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class HeaterService(
    private val heaterRepository: HeaterRepository,
) {
    fun findByOwnerId(ownerId: Long): List<Heater> =
        heaterRepository.findByOwnerId(ownerId)

    fun findBySerial(serial: String): Heater =
        heaterRepository.findBySerial(serial) ?: throw HeaterNotFoundException()

    fun findAll(): List<Heater> =
        heaterRepository.findAll()
}

class HeaterNotFoundException : ResponseStatusException(HttpStatus.NOT_FOUND, "Heater not found")
