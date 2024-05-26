package org.kry.thesis.repository

import org.kry.thesis.domain.Heater
import org.kry.thesis.domain.Schedule
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface ScheduleRepository : JpaRepository<Schedule, Long> {
    fun findByHeaterAndScheduleDate(heater: Heater, date: LocalDate): Schedule?
}
