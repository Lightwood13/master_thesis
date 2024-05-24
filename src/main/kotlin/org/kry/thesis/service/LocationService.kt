package org.kry.thesis.service

import org.kry.thesis.domain.Location
import org.kry.thesis.repository.LocationRepository
import org.springframework.stereotype.Service

@Service
class LocationService(
    private val locationRepository: LocationRepository,
) {
    fun findAll(): List<Location> =
        locationRepository.findAll()
}
