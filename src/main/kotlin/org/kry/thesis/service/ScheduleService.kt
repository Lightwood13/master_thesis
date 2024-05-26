package org.kry.thesis.service

import org.kry.thesis.domain.Heater
import org.kry.thesis.domain.Schedule
import org.kry.thesis.repository.ScheduleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional
class ScheduleService(
    private val scheduleRepository: ScheduleRepository,
) {
    fun findByHeaterAndScheduleDate(heater: Heater, scheduleDate: LocalDate): Schedule? =
        scheduleRepository.findByHeaterAndScheduleDate(heater, scheduleDate)

    fun save(schedule: Schedule): Schedule =
        scheduleRepository.save(schedule)
}
