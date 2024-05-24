package org.kry.thesis.service

import org.kry.thesis.domain.Heater
import org.kry.thesis.domain.Model
import org.kry.thesis.domain.ModelStatus
import org.kry.thesis.repository.ModelRepository
import org.kry.thesis.service.facade.NewModelDTO
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional
class ModelService(
    private val modelRepository: ModelRepository
) {
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
