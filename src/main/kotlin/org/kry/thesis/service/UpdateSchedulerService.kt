package org.kry.thesis.service

import org.kry.thesis.domain.Country
import org.kry.thesis.domain.Location
import org.kry.thesis.service.influxdb.InfluxDBService
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.http.HttpStatus
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

@Component
@Transactional
class UpdateSchedulerService(
    private val weatherService: WeatherService,
    private val locationService: LocationService,
    private val countryService: CountryService,
    private val influxDBService: InfluxDBService,
    private val priceService: PriceService,
) {

    @EventListener(ApplicationReadyEvent::class)
    @Scheduled(fixedDelay = 30, timeUnit = TimeUnit.MINUTES)
    fun updateLocationsWeather() {
        locationService.findAll()
            .forEach { updateLocationWeather(it) }
    }

    @EventListener(ApplicationReadyEvent::class)
    @Scheduled(fixedDelay = 30, timeUnit = TimeUnit.MINUTES)
    fun updateCountriesPrices() {
        countryService.findAll()
            .forEach { updateCountryPrices(it) }
    }

    fun updateLocationWeather(location: Location) {
        val timezone = ZoneId.of(location.country.timezone)
        val currentDate = LocalDate.now(timezone)
        val endDateInclusive = currentDate.plusDays(1)

        if (location.lastUpdated == endDateInclusive) {
            return
        }

        val startDate = location.lastUpdated ?: currentDate.minusDays(5)
        val temperatures: List<Float> = weatherService.getWeather(location, startDate, endDateInclusive)
        val timestamps = generateTimestamps(startDate, endDateInclusive, timezone)
        influxDBService.saveOutsideTemperature(location, temperatures, timestamps)

        location.lastUpdated = endDateInclusive
    }

    fun updateCountryPrices(country: Country) {
        val timezone = ZoneId.of(country.timezone)
        val currentDate = LocalDate.now(timezone)
        val endDateInclusive = currentDate.plusDays(1)

        if (country.lastUpdated == endDateInclusive) {
            return
        }

        val startDate = country.lastUpdated ?: currentDate.minusDays(5)
        val prices: List<Float> = priceService.getPrices(country, startDate, endDateInclusive)
        val timestamps = generateTimestamps(startDate, endDateInclusive, timezone)
        influxDBService.savePrices(country, prices, timestamps)

        country.lastUpdated = endDateInclusive
    }
}

private fun generateTimestamps(startDate: LocalDate, endDateInclusive: LocalDate, timezone: ZoneId): List<Instant> =
    generateLocalTimestamps(startDate, endDateInclusive).map {
        it.atZone(timezone).toInstant()
    }

private fun generateLocalTimestamps(startDate: LocalDate, endDateInclusive: LocalDate): List<LocalDateTime> {
    val result = mutableListOf<LocalDateTime>()
    var cur = startDate.atStartOfDay()
    while (cur.toLocalDate() <= endDateInclusive) {
        result += cur
        cur = cur.plusHours(1)
    }
    return result
}

class HeaterLocationNotFoundException : ResponseStatusException(HttpStatus.NOT_FOUND, "Heater has no location")
