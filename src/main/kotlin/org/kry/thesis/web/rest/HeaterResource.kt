package org.kry.thesis.web.rest

import org.kry.thesis.domain.Heater
import org.kry.thesis.domain.Model
import org.kry.thesis.service.PriceService
import org.kry.thesis.service.facade.HeaterDTO
import org.kry.thesis.service.facade.HeaterFacade
import org.kry.thesis.service.facade.NewModelDTO
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class HeaterResource(
    private val heaterFacade: HeaterFacade,
    private val priceService: PriceService
) {
    @GetMapping("/heaters")
    fun getCurrentUserHeaters(): List<Heater> =
        heaterFacade.getCurrentUserHeaters()

    @GetMapping("/heaters/{serial}")
    fun getHeater(@PathVariable serial: String): HeaterDTO =
        heaterFacade.getBySerialForCurrentUser(serial)

    @PostMapping("/heaters/{serial}/start-calibration")
    fun startCalibration(@PathVariable serial: String) {
        heaterFacade.startCalibration(serial)
    }

    @GetMapping("/heaters/{serial}/models")
    fun getHeaterModels(@PathVariable serial: String): List<Model> =
        heaterFacade.getHeaterModels(serial)

    @PostMapping("/heaters/{serial}/models")
    fun createModel(@PathVariable serial: String, @RequestBody newModelDTO: NewModelDTO) {
        heaterFacade.createModel(serial, newModelDTO)
    }
}
