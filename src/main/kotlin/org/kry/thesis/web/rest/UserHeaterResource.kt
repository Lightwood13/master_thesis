package org.kry.thesis.web.rest

import org.kry.thesis.domain.*
import org.kry.thesis.security.ADMIN
import org.kry.thesis.service.facade.*
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/heaters")
class UserHeaterResource(
    private val heaterFacade: HeaterFacade,
) {
    @GetMapping
    fun getCurrentUserHeaters(): List<Heater> =
        heaterFacade.getCurrentUserHeaters()

    @GetMapping("/{serial}")
    fun getHeater(@PathVariable serial: String): HeaterDTO =
        heaterFacade.getBySerialForCurrentUser(serial)

    @PostMapping
    @PreAuthorize("hasAuthority(\"$ADMIN\")")
    fun createHeater(@RequestBody newHeaterDTO: NewHeaterDTO): Unit =
        heaterFacade.createHeater(newHeaterDTO)

    @PostMapping("/add")
    fun addHeater(@RequestBody addHeaterDTO: AddHeaterDTO): Unit =
        heaterFacade.addHeaterToCurrentUser(addHeaterDTO)

    @PostMapping("/{serial}/start-calibration")
    fun startCalibration(@PathVariable serial: String) {
        heaterFacade.startCalibration(serial)
    }

    @GetMapping("/{serial}/models")
    fun getHeaterModels(@PathVariable serial: String): List<Model> =
        heaterFacade.getHeaterModels(serial)

    @PostMapping("/{serial}/models")
    fun createModel(@PathVariable serial: String, @RequestBody newModelDTO: NewModelDTO) {
        heaterFacade.createModel(serial, newModelDTO)
    }
}
