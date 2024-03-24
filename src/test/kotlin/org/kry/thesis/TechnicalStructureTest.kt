package org.kry.thesis

import com.tngtech.archunit.base.DescribedPredicate.alwaysTrue
import com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.library.Architectures.layeredArchitecture

@AnalyzeClasses(packagesOf = [ThesisApp::class], importOptions = [DoNotIncludeTests::class])
class TechnicalStructureTest {
    @ArchTest
    val respectsTechnicalArchitectureLayers = layeredArchitecture()
        .layer("Config").definedBy("..config..")
        .layer("Web").definedBy("..web..")
        .optionalLayer("Service").definedBy("..service..")
        .layer("Security").definedBy("..security..")
        .layer("Persistence").definedBy("..repository..")
        .layer("Domain").definedBy("..domain..")
        .whereLayer("Config").mayNotBeAccessedByAnyLayer()
        .whereLayer("Web").mayOnlyBeAccessedByLayers("Config")
        .whereLayer("Service").mayOnlyBeAccessedByLayers("Web", "Config")
        .whereLayer("Security").mayOnlyBeAccessedByLayers("Config", "Service", "Web")
        .whereLayer("Persistence").mayOnlyBeAccessedByLayers("Service", "Security", "Web", "Config")
        .whereLayer("Domain").mayOnlyBeAccessedByLayers("Persistence", "Service", "Security", "Web", "Config")
        .ignoreDependency(belongToAnyOf(ThesisApp::class.java), alwaysTrue())
        .ignoreDependency(
            alwaysTrue(),
            belongToAnyOf(
                org.kry.thesis.config.ApplicationProperties::class.java
            )
        )
}
