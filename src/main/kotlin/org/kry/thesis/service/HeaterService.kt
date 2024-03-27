package org.kry.thesis.service

import org.kry.thesis.domain.Heater
import org.kry.thesis.repository.HeaterRepository
import org.springframework.stereotype.Service

@Service
class HeaterService(
    private val heaterRepository: HeaterRepository,
    private val userService: UserService
) {
    fun getCurrentUserHeaters(): List<Heater> {
        val user = userService.getCurrentUser()
        return heaterRepository.getHeatersByOwnerId(user.id!!)
    }
}
