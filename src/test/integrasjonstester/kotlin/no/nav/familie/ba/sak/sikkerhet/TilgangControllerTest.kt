package no.nav.familie.ba.sak.sikkerhet

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.fake.FakePersonopplysningerService.Companion.leggTilPersonInfo
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.mock.FakeFamilieIntegrasjonerTilgangskontrollKlient
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class TilgangControllerTest(
    @Autowired
    private val tilgangController: TilgangController,
    @Autowired
    private val fakeFamilieIntegrasjonerTilgangskontrollKlient: FakeFamilieIntegrasjonerTilgangskontrollKlient,
) : AbstractSpringIntegrationTest() {
    @Test
    fun testHarTilgangTilKode6Person() {
        val fødselsdato = LocalDate.now()
        val fnr =
            leggTilPersonInfo(
                fødselsdato,
                PersonInfo(fødselsdato = fødselsdato, adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG),
            )

        fakeFamilieIntegrasjonerTilgangskontrollKlient.leggTilTilganger(listOf(Tilgang(fnr, true)))

        val response = tilgangController.hentTilgangOgDiskresjonskode(TilgangRequestDTO(fnr))
        val tilgangDTO = response.body?.data ?: throw Feil("Fikk ikke forventet respons")
        assertThat(tilgangDTO.adressebeskyttelsegradering).isEqualTo(ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG)
        assertThat(tilgangDTO.saksbehandlerHarTilgang).isEqualTo(true)

        fakeFamilieIntegrasjonerTilgangskontrollKlient.reset()
    }
}
