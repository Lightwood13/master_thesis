package org.kry.thesis.service

import org.kry.thesis.domain.Country
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class PriceService(
    private val restTemplate: RestTemplate
) {
    fun getPrices(country: Country, startDate: LocalDate, endDateInclusive: LocalDate): List<Float> {
        val result = mutableListOf<Float>()
        var curDate = startDate
        while (curDate.isBefore(endDateInclusive.plusDays(1))) {
            result += getPrices(country, curDate)
            curDate = curDate.plusDays(1)
        }
        return result
    }

    fun getPrices(country: Country, date: LocalDate): List<Float> {
        val headers = HttpHeaders().apply {
            set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        }
        val entity = HttpEntity<Any>(headers)
        val areaCode = country.code

        val urlTemplate = UriComponentsBuilder.fromHttpUrl("https://dataportal-api.nordpoolgroup.com/api/DayAheadPrices")
            .queryParam("date", "{date}")
            .queryParam("market", "{market}")
            .queryParam("deliveryArea", "{deliveryArea}")
            .queryParam("currency", "{currency}")
            .encode()
            .toUriString()
        val params = mapOf(
            "date" to date.format(DateTimeFormatter.ISO_LOCAL_DATE),
            "market" to "DayAhead",
            "deliveryArea" to areaCode,
            "currency" to "EUR"
        )

        return restTemplate.exchange(
            urlTemplate,
            HttpMethod.GET,
            entity,
            PriceResponse::class.java,
            params
        ).body!!.multiAreaEntries.map {
            it.entryPerArea[areaCode]!!
        }
    }
}

private data class PriceResponse(
    val multiAreaEntries: List<MultiAreaEntry>
)

private data class MultiAreaEntry(
    val entryPerArea: Map<String, Float>
)
