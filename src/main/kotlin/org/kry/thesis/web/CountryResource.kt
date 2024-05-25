package org.kry.thesis.web

import org.kry.thesis.domain.Country
import org.kry.thesis.service.CountryService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/countries")
class CountryResource(private val countryService: CountryService) {
    @GetMapping
    fun getCountries(): List<Country> =
        countryService.findAll()
}
