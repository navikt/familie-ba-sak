package no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.eøs.assertEqualsUnordered
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration
import javax.validation.ConstraintViolationException

@SpringBootTest
@ContextConfiguration(classes = [TestConfig::class, UtenlandskPeriodebeløpController::class, ValidationAutoConfiguration::class])
class UtenlandskPeriodebeløpControllerTest {

    @Autowired
    lateinit var featureToggleService: FeatureToggleService

    @Autowired
    lateinit var utenlandskPeriodebeløpController: UtenlandskPeriodebeløpController

    @Test
    fun `Skal kaste feil dersom validering av input feiler`() {
        val exception = assertThrows<ConstraintViolationException> { utenlandskPeriodebeløpController.oppdaterUtenlandskPeriodebeløp(1, RestUtenlandskPeriodebeløp(1, null, null, emptyList(), beløp = (-1.0).toBigDecimal(), null, null, null)) }

        val forventedeFelterMedFeil = listOf("beløp")
        val faktiskeFelterMedFeil = exception.constraintViolations.map { constraintViolation -> constraintViolation.propertyPath.toString().split(".").last() }

        assertEqualsUnordered(forventedeFelterMedFeil, faktiskeFelterMedFeil)

        println(faktiskeFelterMedFeil)
    }

    @Test
    fun `Skal ikke kaste feil dersom validering av input går bra`() {

        // Stopper videre prosessering etter at validering er gjennomført
        every { featureToggleService.isEnabled(any()) }.returns(false)

        val response = utenlandskPeriodebeløpController.oppdaterUtenlandskPeriodebeløp(1, RestUtenlandskPeriodebeløp(1, null, null, emptyList(), beløp = 1.0.toBigDecimal(), null, null, null))

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.statusCode)
    }
}

class TestConfig {

    @Bean
    fun featureToggleService(): FeatureToggleService = mockk()

    @Bean
    fun utenlandskPeriodebeløpService(): UtenlandskPeriodebeløpService = mockk()

    @Bean
    fun utenlandskPeriodebeløpRepository(): UtenlandskPeriodebeløpRepository = mockk()

    @Bean
    fun personidentService(): PersonidentService = mockk()

    @Bean
    fun utvidetBehandlingService(): UtvidetBehandlingService = mockk()
}
