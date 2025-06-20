package no.nav.familie.ba.sak.integrasjoner.ainntekt

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.util.BrukerContextUtil
import no.nav.familie.kontrakter.felles.PersonIdent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class AInntektControllerTest(
    @Autowired
    private val ainntektController: AInntektController,
) : AbstractSpringIntegrationTest() {
    private val ainntektUrl = "/test/1234"

    @BeforeEach
    fun init() {
        BrukerContextUtil.mockBrukerContext()
    }

    @AfterEach
    fun tearDown() {
        BrukerContextUtil.clearBrukerContext()
    }

    @Test
    fun `kan hente ut A-Inntekt url`() {
        val responseRessurs = ainntektController.hentAInntektUrl(PersonIdent(ident = randomFnr()))
        assertThat(responseRessurs.data).isEqualTo(ainntektUrl)
    }

    @Test
    fun `prøver å hente ut A-Inntekt url på et ugyldig fnr`() {
        val feil =
            assertThrows<IllegalStateException> {
                ainntektController.hentAInntektUrl(PersonIdent(ident = "10000111111"))
            }

        assertThat(feil.message).contains("10000111111")
    }
}
