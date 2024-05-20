package org.kry.thesis.web.rest

import org.kry.thesis.domain.Heater
import org.kry.thesis.service.facade.HeaterDTO
import org.kry.thesis.service.facade.HeaterFacade
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class HeaterResource(
    private val heaterFacade: HeaterFacade
) {
    @GetMapping("/heaters")
    fun getCurrentUserHeaters(): List<Heater> =
        heaterFacade.getCurrentUserHeaters()

    @GetMapping("/heaters/{serial}")
    fun getHeater(@PathVariable serial: String): HeaterDTO =
        heaterFacade.getBySerialForCurrentUser(serial)
}
