package no.nav.familie.ba.sak.config

import no.finn.unleash.UnleashContext
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

internal class ByEnvironmentStrategyTest {
    private val map = ByEnvironmentStrategy.lagPropertyMapMedMiljø("preprod-fss", "prod-fss")

    @Test
    fun `skal svare true for riktig miljø`() {

        Assertions.assertThat(
                ByEnvironmentStrategy().isEnabled(map, UnleashContext.builder().environment("preprod-fss").build())
        ).isTrue()
    }

    @Test
    fun `skal svare false for miljø som ikke er definert`() {

        Assertions.assertThat(
                ByEnvironmentStrategy().isEnabled(map, UnleashContext.builder().environment("miljø-som-ikke-finnes").build())
        ).isFalse()
    }
}