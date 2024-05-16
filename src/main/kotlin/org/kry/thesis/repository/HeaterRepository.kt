package org.kry.thesis.repository

import org.kry.thesis.domain.Heater
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface HeaterRepository : JpaRepository<Heater, Long> {
    fun getHeatersByOwnerId(ownerId: Long): List<Heater>
    fun findBySerial(serial: String): Heater?
}
