package org.kry.thesis.web.rest

import org.kry.thesis.domain.Country
import org.kry.thesis.service.CountryService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/countries")
class CountryResource(private val countryService: CountryService) {
    @GetMapping
    fun getCountries(): List<Country> =
        countryService.findAll()
}
