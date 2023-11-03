package no.nav.familie.ba.sak.config.featureToggle

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@Tag("integration")
class UnleashNextTest(
    @Autowired
    private val unleashNext: UnleashNextMedContextService,
) : AbstractSpringIntegrationTest() {

    @Test
    fun `skal svare true ved dummy impl i tester`() {
        Assertions.assertEquals(true, unleashNext.isEnabled("sull-bala-tull"))
    }
}
