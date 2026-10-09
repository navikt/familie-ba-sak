package no.nav.familie.ba.sak.sikkerhet

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.datagenerator.lagPersonTilgangAvvistGrunnetStrengtFortrolig
import no.nav.familie.ba.sak.ekstern.restDomene.TilgangDTO
import no.nav.familie.ba.sak.fake.FakePersonopplysningerService.Companion.leggTilPersonInfo
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.mock.FakeFamilieIntegrasjonerTilgangskontrollKlient
import no.nav.familie.ba.sak.mock.FakeTilgangsmaskinTilgangskontrollKlient
import no.nav.familie.ba.sak.util.BrukerContextUtil
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class TilgangControllerTest(
    @Autowired
    private val tilgangController: TilgangController,
    @Autowired
    private val fakeTilgangsmaskinTilgangskontrollKlient: FakeTilgangsmaskinTilgangskontrollKlient,
    @Autowired
    private val fakeFamilieIntegrasjonerTilgangskontrollKlient: FakeFamilieIntegrasjonerTilgangskontrollKlient,
) : AbstractSpringIntegrationTest() {
    @BeforeEach
    fun setUp() {
        BrukerContextUtil.mockBrukerContext()
    }

    @AfterEach
    fun tearDown() {
        BrukerContextUtil.clearBrukerContext()
        fakeTilgangsmaskinTilgangskontrollKlient.reset()
        fakeFamilieIntegrasjonerTilgangskontrollKlient.reset()
        System.clearProperty(FeatureToggle.SKAL_BRUKE_TILGANGSMASKINEN.navn)
    }

    @Test
    fun `skal gi tilgang til person med strengt fortrolig adresse når Tilgangsmaskinen gir tilgang`() {
        // Arrange
        val fnr = leggTilPersonMedStrengtFortroligAdresse()
        fakeTilgangsmaskinTilgangskontrollKlient.leggTilTilganger(listOf(PersonTilgang.medTilgang(fnr)))

        // Act
        val tilgangDTO = hentTilgang(fnr)

        // Assert
        assertThat(tilgangDTO.adressebeskyttelsegradering).isEqualTo(ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG)
        assertThat(tilgangDTO.saksbehandlerHarTilgang).isTrue()
        assertThat(fakeTilgangsmaskinTilgangskontrollKlient.antallKallTilSjekkTilgangTilPersoner()).isEqualTo(1)
    }

    @Test
    fun `skal ikke gi tilgang til person med strengt fortrolig adresse når Tilgangsmaskinen avviser`() {
        // Arrange
        val fnr = leggTilPersonMedStrengtFortroligAdresse()
        fakeTilgangsmaskinTilgangskontrollKlient.leggTilTilganger(listOf(lagPersonTilgangAvvistGrunnetStrengtFortrolig(fnr)))

        // Act
        val tilgangDTO = hentTilgang(fnr)

        // Assert
        assertThat(tilgangDTO.adressebeskyttelsegradering).isEqualTo(ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG)
        assertThat(tilgangDTO.saksbehandlerHarTilgang).isFalse()
    }

    @Test
    fun `skal sjekke tilgangen mot familie-integrasjoner når togglen for Tilgangsmaskinen er av`() {
        // Arrange
        System.setProperty(FeatureToggle.SKAL_BRUKE_TILGANGSMASKINEN.navn, "false")
        val fnr = leggTilPersonMedStrengtFortroligAdresse()
        fakeFamilieIntegrasjonerTilgangskontrollKlient.leggTilTilganger(listOf(Tilgang(personIdent = fnr, harTilgang = false)))

        // Act
        val tilgangDTO = hentTilgang(fnr)

        // Assert
        assertThat(tilgangDTO.saksbehandlerHarTilgang).isFalse()
        assertThat(fakeFamilieIntegrasjonerTilgangskontrollKlient.hentKallMotSjekkTilgangTilPersoner()).containsExactly(listOf(fnr))
        assertThat(fakeTilgangsmaskinTilgangskontrollKlient.antallKallTilSjekkTilgangTilPersoner()).isZero()
    }

    private fun leggTilPersonMedStrengtFortroligAdresse(): String {
        val fødselsdato = LocalDate.now()
        return leggTilPersonInfo(
            fødselsdato,
            PersonInfo(fødselsdato = fødselsdato, adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG),
        )
    }

    private fun hentTilgang(fnr: String): TilgangDTO = tilgangController.hentTilgangOgDiskresjonskode(TilgangRequestDTO(fnr)).body?.data ?: throw Feil("Fikk ikke forventet respons")
}
