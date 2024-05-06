package no.nav.familie.ba.sak.sikkerhet

import io.mockk.every
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.MockPersonopplysningerService.Companion.leggTilPersonInfo
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollClient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import no.nav.familie.util.FnrGenerator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class TilgangControllerTest(
    @Autowired
    private val tilgangController: TilgangController,
    @Autowired
    private val mockFamilieIntegrasjonerTilgangskontrollClient: FamilieIntegrasjonerTilgangskontrollClient,
) : AbstractSpringIntegrationTest() {
    @Test
    fun testHarTilgangTilKode6Person() {
        val fnr =
            leggTilPersonInfo(
                FnrGenerator.generer(),
                PersonInfo(fødselsdato = LocalDate.now(), adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG),
            )
        every {
            mockFamilieIntegrasjonerTilgangskontrollClient.sjekkTilgangTilPersoner(listOf(fnr))
        } answers { firstArg<List<String>>().map { Tilgang(it, true) } }

        val response = tilgangController.hentTilgangOgDiskresjonskode(TilgangRequestDTO(fnr))
        val tilgangDTO = response.body?.data ?: error("Fikk ikke forventet respons")
        assertThat(tilgangDTO.adressebeskyttelsegradering).isEqualTo(ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG)
        assertThat(tilgangDTO.saksbehandlerHarTilgang).isEqualTo(true)
    }
}
