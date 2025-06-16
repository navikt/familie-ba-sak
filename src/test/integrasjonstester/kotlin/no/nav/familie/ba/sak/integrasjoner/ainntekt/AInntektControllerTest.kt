package no.nav.familie.ba.sak.integrasjoner.ainntekt

import no.nav.familie.ba.sak.common.RolleTilgangskontrollFeil
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.BARN_DET_IKKE_GIS_TILGANG_TIL_FNR
import no.nav.familie.ba.sak.config.INTEGRASJONER_FNR
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
        val responseRessurs = ainntektController.hentAInntektUrl(PersonIdent(ident = INTEGRASJONER_FNR))
        assertThat(responseRessurs.data).isEqualTo(ainntektUrl)
    }

    @Test
    fun `har ikke tilgang til å hente ut A-Inntekt url`() {
        val feil =
            assertThrows<RolleTilgangskontrollFeil> {
                ainntektController.hentAInntektUrl(PersonIdent(ident = BARN_DET_IKKE_GIS_TILGANG_TIL_FNR))
            }

        assertThat(feil.message).contains("Saksbehandler A har ikke tilgang.")
    }
}
