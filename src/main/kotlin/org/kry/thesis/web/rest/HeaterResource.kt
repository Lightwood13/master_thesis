package org.kry.thesis.web.rest

import org.kry.thesis.security.HEATER
import org.kry.thesis.service.facade.*
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/heater")
@PreAuthorize("hasAuthority(\"$HEATER\")")
class HeaterResource(private val heaterFacade: HeaterFacade) {
    @GetMapping
    fun getCurrentHeater(): HeaterDTO =
        heaterFacade.getCurrentHeater()

    @GetMapping("/schedule")
    fun getSchedule(@RequestParam date: LocalDate): ScheduleDTO =
        ScheduleDTO(
            schedule = heaterFacade.getScheduleForCurrentHeater(date)?.data
        )
}

data class ScheduleDTO(
    val schedule: String?
)
