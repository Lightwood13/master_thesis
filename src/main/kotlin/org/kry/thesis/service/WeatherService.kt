package org.kry.thesis.service

import org.kry.thesis.domain.Location
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Service
class WeatherService(
    private val restTemplate: RestTemplate
) {
    fun getWeather(location: Location, startDate: LocalDate, endDateInclusive: LocalDate): List<Float> {
        val headers = HttpHeaders().apply {
            set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        }
        val entity = HttpEntity<Any>(headers)
        val url = chooseApiUrl(endDateInclusive, location.country.timezone)

        val urlTemplate = UriComponentsBuilder.fromHttpUrl(url)
            .queryParam("latitude", "{latitude}")
            .queryParam("longitude", "{longitude}")
            .queryParam("start_date", "{start_date}")
            .queryParam("end_date", "{end_date}")
            .queryParam("hourly", "{hourly}")
            .queryParam("timezone", "{timezone}")
            .encode()
            .toUriString()
        val params = mapOf(
            "latitude" to location.latitude.toString(),
            "longitude" to location.longitude.toString(),
            "start_date" to startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
            "end_date" to endDateInclusive.format(DateTimeFormatter.ISO_LOCAL_DATE),
            "hourly" to "temperature_2m",
            "timezone" to location.country.timezone
        )

        return restTemplate.exchange(
            urlTemplate,
            HttpMethod.GET,
            entity,
            WeatherResponse::class.java,
            params
        ).body!!.hourly.temperature_2m
    }

    private fun chooseApiUrl(endDateInclusive: LocalDate, timezone: String): String {
        val currentDate = LocalDateTime.now(ZoneId.of(timezone))
        return if (ChronoUnit.DAYS.between(endDateInclusive, currentDate) >= HISTORICAL_DATA_LAG_DAYS) {
            "https://archive-api.open-meteo.com/v1/archive"
        } else {
            "https://api.open-meteo.com/v1/forecast"
        }
    }
}

private const val HISTORICAL_DATA_LAG_DAYS: Int = 7

private data class WeatherResponse(
    val hourly: HourlyWeatherData
)

private data class HourlyWeatherData(
    val temperature_2m: List<Float>
)
