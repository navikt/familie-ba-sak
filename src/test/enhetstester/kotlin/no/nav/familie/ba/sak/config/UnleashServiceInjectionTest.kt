package no.nav.familie.ba.sak.config

import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.unleash.UnleashService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service

class UnleashServiceInjectionTest {
    @Test
    fun `UnleashService skal kun være injected i FeatureToggleService`() {
        val scanner =
            ClassPathScanningCandidateComponentProvider(false).apply {
                addIncludeFilter(AnnotationTypeFilter(Service::class.java))
                addIncludeFilter(AnnotationTypeFilter(Component::class.java))
            }

        val classesWithUnleashServiceInjection =
            scanner
                .findCandidateComponents("no.nav.familie.ba.sak")
                .map { Class.forName(it.beanClassName) }
                .filter { clazz ->
                    val hasConstructorInjection =
                        clazz.declaredConstructors.any { constructor ->
                            constructor.parameterTypes.any {
                                it.simpleName == UnleashService::class.simpleName
                            }
                        }

                    val hasFieldInjection =
                        clazz.declaredFields.any { field ->
                            field.type.simpleName == UnleashService::class.simpleName
                        }

                    hasConstructorInjection || hasFieldInjection
                }.map { it.simpleName }

        assertThat(classesWithUnleashServiceInjection)
            .describedAs(
                "UnleashService er feilaktig injected. Bruk FeatureToggleService i stedet.",
                FeatureToggleService::class.simpleName,
                classesWithUnleashServiceInjection,
            ).containsOnly(FeatureToggleService::class.simpleName)
    }
}
