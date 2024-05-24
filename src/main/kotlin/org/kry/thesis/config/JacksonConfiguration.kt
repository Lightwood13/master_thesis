package org.kry.thesis.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.zalando.problem.jackson.ProblemModule
import org.zalando.problem.violations.ConstraintViolationProblemModule

@Configuration
class JacksonConfiguration {

    /**
     * Support for Java date and time API.
     * @return the corresponding Jackson module.
     */
    @Bean
    fun javaTimeModule() = JavaTimeModule()

    @Bean
    fun jdk8TimeModule() = Jdk8Module()

    /*
     * Support for Hibernate types in Jackson.
     */
    @Bean
    fun hibernate5Module() = Hibernate5Module()

    /*
     * Module for serialization/deserialization of RFC7807 Problem.
     */
    @Bean
    fun problemModule() = ProblemModule()

    /*
     * Module for serialization/deserialization of ConstraintViolationProblem.
     */
    @Bean
    fun constraintViolationProblemModule() = ConstraintViolationProblemModule()

    @Bean
    fun objectMapper(): ObjectMapper = ObjectMapper()
        .registerModules(
            javaTimeModule(),
            jdk8TimeModule(),
            hibernate5Module(),
            problemModule(),
            constraintViolationProblemModule(),
            KotlinModule.Builder().build()
        ).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
}
