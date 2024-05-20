package org.kry.thesis.repository

import org.kry.thesis.domain.Heater
import org.kry.thesis.domain.LatestMetrics
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface LatestMetricsRepository : JpaRepository<LatestMetrics, Long> {
    fun findLatestMetricsByHeater(heater: Heater): LatestMetrics?
}
