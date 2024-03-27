package org.kry.thesis.config

import com.influxdb.client.InfluxDBClient
import com.influxdb.client.InfluxDBClientFactory
import com.influxdb.client.InfluxDBClientOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class InfluxDBConfiguration(
    @Value("\${influxdb.address}") private val influxDBAddress: String,
    @Value("\${influxdb.token}") private val influxDBToken: String,
    @Value("\${influxdb.org}") private val influxDBOrganization: String,
    @Value("\${influxdb.bucket}") private val influxDBBucket: String
) {

    @Bean
    fun influxDBClient(): InfluxDBClient =
        InfluxDBClientFactory.create(
            InfluxDBClientOptions.builder()
                .url("http://$influxDBAddress")
                .org(influxDBOrganization)
                .bucket(influxDBBucket)
                .authenticateToken(influxDBToken.toCharArray())
                .build()
        )
}
