package org.kry.thesis.web.rest

import org.kry.thesis.domain.Heater
import org.kry.thesis.service.HeaterService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class HeaterResource(
    private val heaterService: HeaterService
) {
    @GetMapping("/heaters")
    fun getCurrentUserHeaters(): List<Heater> =
        heaterService.getCurrentUserHeaters()

    @GetMapping("/heaters/{serial}")
    fun getHeater(@PathVariable serial: String): Heater =
        heaterService.getBySerialForCurrentUser(serial)
}
