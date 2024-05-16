package org.kry.thesis.service

import org.kry.thesis.domain.Heater
import org.kry.thesis.repository.HeaterRepository
import org.kry.thesis.service.facade.HeaterAccessForbiddenException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class HeaterService(
    private val heaterRepository: HeaterRepository,
    private val userService: UserService
) {
    fun getCurrentUserHeaters(): List<Heater> {
        val user = userService.getCurrentUser()
        return heaterRepository.getHeatersByOwnerId(user.id!!)
    }

    fun getBySerialForCurrentUser(serial: String): Heater {
        val user = userService.getCurrentUser()
        val heater = findBySerial(serial)
        if (heater.owner != user) {
            throw HeaterAccessForbiddenException()
        }
        return heater
    }

    fun findBySerial(serial: String): Heater =
        heaterRepository.findBySerial(serial) ?: throw HeaterNotFoundException()
}

class HeaterNotFoundException : ResponseStatusException(HttpStatus.NOT_FOUND, "Heater not found")
