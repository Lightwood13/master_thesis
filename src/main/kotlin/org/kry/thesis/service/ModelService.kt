package org.kry.thesis.service

import org.kry.thesis.domain.Heater
import org.kry.thesis.domain.Model
import org.kry.thesis.domain.ModelStatus
import org.kry.thesis.repository.ModelRepository
import org.kry.thesis.service.facade.NewModelDTO
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

@Service
@Transactional
class ModelService(
    private val modelRepository: ModelRepository
) {
    fun findById(id: Long): Model =
        modelRepository.findByIdOrNull(id) ?: throw ModelNotFoundException()

    fun createNewModel(heater: Heater, newModelDTO: NewModelDTO, createdOn: Instant): Model =
        modelRepository.saveAndFlush(
            Model(
                heater = heater,
                name = newModelDTO.name,
                targetTemperature = newModelDTO.targetTemperature,
                minTemperature = newModelDTO.minTemperature,
                maxTemperature = newModelDTO.maxTemperature,
                status = ModelStatus.Training,
                createdOn = createdOn
            )
        )
}

class ModelNotFoundException : ResponseStatusException(HttpStatus.NOT_FOUND, "Model not found")
