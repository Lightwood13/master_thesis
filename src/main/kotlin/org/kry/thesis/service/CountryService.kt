package org.kry.thesis.service

import org.kry.thesis.domain.Country
import org.kry.thesis.repository.CountryRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class CountryService(
    private val countryRepository: CountryRepository,
) {
    fun findById(id: Long): Country =
        countryRepository.findByIdOrNull(id) ?: throw CountryNotFoundException()

    fun findAll(): List<Country> =
        countryRepository.findAll()
}

class CountryNotFoundException : ResponseStatusException(HttpStatus.NOT_FOUND, "Country not found")
