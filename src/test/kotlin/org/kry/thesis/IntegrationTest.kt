package org.kry.thesis

import org.kry.thesis.config.AsyncSyncConfiguration
import org.kry.thesis.config.EmbeddedKafka
import org.kry.thesis.config.EmbeddedSQL
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext

/**
 * Base composite annotation for integration tests.
 */
@kotlin.annotation.Target(AnnotationTarget.CLASS)
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@SpringBootTest(classes = [ThesisApp::class, AsyncSyncConfiguration::class])
@EmbeddedKafka
@EmbeddedSQL
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
annotation class IntegrationTest
