package org.kry.thesis.service

import org.kry.thesis.domain.Country
import org.kry.thesis.repository.CountryRepository
import org.springframework.stereotype.Service

@Service
class CountryService(
    private val countryRepository: CountryRepository,
) {
    fun findAll(): List<Country> =
        countryRepository.findAll()
}
