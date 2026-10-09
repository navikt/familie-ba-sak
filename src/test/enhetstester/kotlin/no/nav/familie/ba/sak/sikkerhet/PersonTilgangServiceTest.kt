package no.nav.familie.ba.sak.sikkerhet

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.clearAllCaches
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.datagenerator.lagPersonTilgangAvvistGrunnetSkjerming
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollKlient
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestKlient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlAdressebeskyttelsePerson
import no.nav.familie.ba.sak.integrasjoner.tilgangsmaskin.TilgangsmaskinTilgangskontrollKlient
import no.nav.familie.ba.sak.mock.FakeFamilieIntegrasjonerTilgangskontrollKlient
import no.nav.familie.ba.sak.mock.FakeTilgangsmaskinTilgangskontrollKlient
import no.nav.familie.ba.sak.util.BrukerContextUtil.testWithBrukerContext
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.Adressebeskyttelse
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import no.nav.familie.tilgangsmaskin.Avvisningskode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.cache.concurrent.ConcurrentMapCacheManager

class PersonTilgangServiceTest {
    private val fakeTilgangsmaskinTilgangskontrollKlient = FakeTilgangsmaskinTilgangskontrollKlient()

    private val fakeFamilieIntegrasjonerTilgangskontrollKlient = FakeFamilieIntegrasjonerTilgangskontrollKlient()

    private val featureToggleService = mockk<FeatureToggleService>()

    private val cacheManager = ConcurrentMapCacheManager()

    private val systemOnlyPdlRestKlient = mockk<SystemOnlyPdlRestKlient>()

    private val service =
        PersonTilgangService(
            fakeTilgangsmaskinTilgangskontrollKlient,
            fakeFamilieIntegrasjonerTilgangskontrollKlient,
            featureToggleService,
            cacheManager,
            systemOnlyPdlRestKlient,
        )

    @BeforeEach
    fun setUp() {
        cacheManager.clearAllCaches()
        every { featureToggleService.isEnabled(FeatureToggle.SKAL_BRUKE_TILGANGSMASKINEN) } returns true
    }

    @AfterEach
    fun tearDown() {
        fakeTilgangsmaskinTilgangskontrollKlient.reset()
        fakeFamilieIntegrasjonerTilgangskontrollKlient.reset()
    }

    @Test
    fun `skal sjekke tilgang mot Tilgangsmaskinen når toggle er på`() {
        // Arrange
        fakeTilgangsmaskinTilgangskontrollKlient.leggTilTilganger(listOf(lagPersonTilgangAvvistGrunnetSkjerming("1")))

        // Act
        val tilgang = testWithBrukerContext { service.sjekkTilgangTilPerson("1") }

        // Assert
        assertThat(tilgang).isEqualTo(lagPersonTilgangAvvistGrunnetSkjerming("1"))
        assertThat(fakeTilgangsmaskinTilgangskontrollKlient.antallKallTilSjekkTilgangTilPersoner()).isEqualTo(1)
        assertThat(fakeFamilieIntegrasjonerTilgangskontrollKlient.antallKallTilSjekkTilgangTilPersoner()).isEqualTo(0)
    }

    @Test
    fun `skal sjekke tilgang mot familie-integrasjoner når toggle er av`() {
        // Arrange
        every { featureToggleService.isEnabled(FeatureToggle.SKAL_BRUKE_TILGANGSMASKINEN) } returns false
        fakeFamilieIntegrasjonerTilgangskontrollKlient.leggTilTilganger(listOf(Tilgang(personIdent = "1", harTilgang = true)))

        // Act
        val tilgang = testWithBrukerContext { service.sjekkTilgangTilPerson("1") }

        // Assert
        assertThat(tilgang).isEqualTo(PersonTilgang.medTilgang("1"))
        assertThat(fakeFamilieIntegrasjonerTilgangskontrollKlient.hentKallMotSjekkTilgangTilPersoner()).containsExactly(listOf("1"))
        assertThat(fakeTilgangsmaskinTilgangskontrollKlient.antallKallTilSjekkTilgangTilPersoner()).isEqualTo(0)
    }

    @Test
    fun `skal gi avvisningskode for strengt fortrolig adresse når familie-integrasjoner avviser grunnet manglende strengt fortrolig-rolle`() {
        // Arrange
        every { featureToggleService.isEnabled(FeatureToggle.SKAL_BRUKE_TILGANGSMASKINEN) } returns false
        val begrunnelse = "Bruker mangler rollen '0000-GA-Strengt_Fortrolig_Adresse'"
        fakeFamilieIntegrasjonerTilgangskontrollKlient.leggTilTilganger(
            listOf(Tilgang(personIdent = "1", harTilgang = false, begrunnelse = begrunnelse)),
        )

        // Act
        val tilgang = testWithBrukerContext { service.sjekkTilgangTilPerson("1") }

        // Assert
        assertThat(tilgang).isEqualTo(PersonTilgang.avvist("1", Avvisningskode.AVVIST_STRENGT_FORTROLIG_ADRESSE, begrunnelse))
        assertThat(tilgang.erAvvistGrunnetStrengtFortrolig()).isTrue
    }

    @Test
    fun `skal gi avvisningskode UKJENT når familie-integrasjoner avviser av andre grunner`() {
        // Arrange
        every { featureToggleService.isEnabled(FeatureToggle.SKAL_BRUKE_TILGANGSMASKINEN) } returns false
        val begrunnelse = "Bruker mangler rollen '0000-GA-Egne_ansatte'"
        fakeFamilieIntegrasjonerTilgangskontrollKlient.leggTilTilganger(
            listOf(Tilgang(personIdent = "1", harTilgang = false, begrunnelse = begrunnelse)),
        )

        // Act
        val tilgang = testWithBrukerContext { service.sjekkTilgangTilPerson("1") }

        // Assert
        assertThat(tilgang).isEqualTo(PersonTilgang.avvist("1", Avvisningskode.UKJENT, begrunnelse))
        assertThat(tilgang.erAvvistGrunnetStrengtFortrolig()).isFalse
    }

    @Test
    fun `skal gi standardbegrunnelse når familie-integrasjoner avviser uten begrunnelse`() {
        // Arrange
        every { featureToggleService.isEnabled(FeatureToggle.SKAL_BRUKE_TILGANGSMASKINEN) } returns false
        fakeFamilieIntegrasjonerTilgangskontrollKlient.leggTilTilganger(listOf(Tilgang(personIdent = "1", harTilgang = false)))

        // Act
        val tilgang = testWithBrukerContext { service.sjekkTilgangTilPerson("1") }

        // Assert
        assertThat(tilgang)
            .isEqualTo(PersonTilgang.avvist("1", Avvisningskode.UKJENT, "familie-integrasjoner ga ikke tilgang til personen"))
    }

    @Test
    fun `skal kaste feil når familie-integrasjoner ikke svarer for alle identer`() {
        // Arrange
        every { featureToggleService.isEnabled(FeatureToggle.SKAL_BRUKE_TILGANGSMASKINEN) } returns false
        val familieIntegrasjonerTilgangskontrollKlient = mockk<FamilieIntegrasjonerTilgangskontrollKlient>()
        val serviceMedUfullstendigSvar =
            PersonTilgangService(
                fakeTilgangsmaskinTilgangskontrollKlient,
                familieIntegrasjonerTilgangskontrollKlient,
                featureToggleService,
                cacheManager,
                systemOnlyPdlRestKlient,
            )
        every { familieIntegrasjonerTilgangskontrollKlient.sjekkTilgangTilPersoner(listOf("1", "2")) } returns
            listOf(Tilgang(personIdent = "1", harTilgang = true))

        // Act
        val feil = assertThrows<Feil> { testWithBrukerContext { serviceMedUfullstendigSvar.sjekkTilgangTilPersoner(listOf("1", "2")) } }

        // Assert
        assertThat(feil.message).isEqualTo("Fikk ikke svar fra familie-integrasjoner for 1 av 2 identer.")
    }

    @Test
    fun `skal ikke bruke tilganger cachet fra den andre kilden når toggle endres`() {
        // Arrange
        fakeTilgangsmaskinTilgangskontrollKlient.leggTilTilganger(listOf(lagPersonTilgangAvvistGrunnetSkjerming("1")))
        fakeFamilieIntegrasjonerTilgangskontrollKlient.leggTilTilganger(listOf(Tilgang(personIdent = "1", harTilgang = true)))
        val tilgangFraTilgangsmaskinen = testWithBrukerContext { service.sjekkTilgangTilPerson("1") }
        every { featureToggleService.isEnabled(FeatureToggle.SKAL_BRUKE_TILGANGSMASKINEN) } returns false

        // Act
        val tilgangFraFamilieIntegrasjoner = testWithBrukerContext { service.sjekkTilgangTilPerson("1") }

        // Assert
        assertThat(tilgangFraTilgangsmaskinen.harTilgang).isFalse
        assertThat(tilgangFraFamilieIntegrasjoner.harTilgang).isTrue
        assertThat(fakeTilgangsmaskinTilgangskontrollKlient.antallKallTilSjekkTilgangTilPersoner()).isEqualTo(1)
        assertThat(fakeFamilieIntegrasjonerTilgangskontrollKlient.antallKallTilSjekkTilgangTilPersoner()).isEqualTo(1)
    }

    @Test
    fun `skal cache at saksbehandler har tilgang`() {
        // Arrange
        fakeTilgangsmaskinTilgangskontrollKlient.leggTilTilganger(listOf(PersonTilgang.medTilgang("1")))
        testWithBrukerContext { service.sjekkTilgangTilPerson("1") }
        fakeTilgangsmaskinTilgangskontrollKlient.leggTilTilganger(listOf(lagPersonTilgangAvvistGrunnetSkjerming("1")))

        // Act
        val tilgang = testWithBrukerContext { service.sjekkTilgangTilPerson("1") }

        // Assert
        assertThat(tilgang.harTilgang).isTrue
        assertThat(fakeTilgangsmaskinTilgangskontrollKlient.antallKallTilSjekkTilgangTilPersoner()).isEqualTo(1)
    }

    @Test
    fun `skal cache at saksbehandler ikke har tilgang`() {
        // Arrange
        fakeTilgangsmaskinTilgangskontrollKlient.leggTilTilganger(listOf(lagPersonTilgangAvvistGrunnetSkjerming("1")))
        testWithBrukerContext { service.sjekkTilgangTilPerson("1") }
        fakeTilgangsmaskinTilgangskontrollKlient.leggTilTilganger(listOf(PersonTilgang.medTilgang("1")))

        // Act
        val tilgang = testWithBrukerContext { service.sjekkTilgangTilPerson("1") }

        // Assert
        assertThat(tilgang.harTilgang).isFalse
        assertThat(fakeTilgangsmaskinTilgangskontrollKlient.antallKallTilSjekkTilgangTilPersoner()).isEqualTo(1)
    }

    @Test
    fun `skal cache tilgang per saksbehandler`() {
        // Arrange
        fakeTilgangsmaskinTilgangskontrollKlient.leggTilTilganger(listOf(lagPersonTilgangAvvistGrunnetSkjerming("1")))
        val tilgangForSaksbehandler1 = testWithBrukerContext("saksbehandler1") { service.sjekkTilgangTilPerson("1") }
        fakeTilgangsmaskinTilgangskontrollKlient.leggTilTilganger(listOf(PersonTilgang.medTilgang("1")))

        // Act
        val tilgangForSaksbehandler2 = testWithBrukerContext("saksbehandler2") { service.sjekkTilgangTilPerson("1") }

        // Assert
        assertThat(tilgangForSaksbehandler1.harTilgang).isFalse
        assertThat(tilgangForSaksbehandler2.harTilgang).isTrue
        assertThat(fakeTilgangsmaskinTilgangskontrollKlient.antallKallTilSjekkTilgangTilPersoner()).isEqualTo(2)
    }

    @Test
    fun `skal gi tilgang uten å kalle Tilgangsmaskinen i systemkontekst`() {
        // Arrange
        fakeTilgangsmaskinTilgangskontrollKlient.leggTilTilganger(listOf(lagPersonTilgangAvvistGrunnetSkjerming("1")))

        // Act
        val tilgang = service.sjekkTilgangTilPerson("1")

        // Assert
        assertThat(tilgang.harTilgang).isTrue
        assertThat(fakeTilgangsmaskinTilgangskontrollKlient.antallKallTilSjekkTilgangTilPersoner()).isEqualTo(0)
    }

    @Test
    fun `skal sjekke tilgang til unike identer`() {
        // Arrange
        fakeTilgangsmaskinTilgangskontrollKlient.leggTilTilganger(listOf(lagPersonTilgangAvvistGrunnetSkjerming("1")))

        // Act
        testWithBrukerContext("saksbehandler1") { service.sjekkTilgangTilPersoner(listOf("1", "1")) }

        // Assert
        assertThat(fakeTilgangsmaskinTilgangskontrollKlient.hentKallMotSjekkTilgangTilPersoner()).containsExactly(setOf("1"))
    }

    @Test
    fun `skal ikke hente identer som allerede finnes i cachen`() {
        // Arrange
        val tilganger =
            listOf(
                lagPersonTilgangAvvistGrunnetSkjerming("1"),
                PersonTilgang.medTilgang("2"),
                lagPersonTilgangAvvistGrunnetSkjerming("3"),
            )
        fakeTilgangsmaskinTilgangskontrollKlient.leggTilTilganger(tilganger)
        testWithBrukerContext { service.sjekkTilgangTilPerson("1") }

        // Act
        val tilgangPerIdent = testWithBrukerContext { service.sjekkTilgangTilPersoner(listOf("2", "1", "3")) }
        testWithBrukerContext { service.sjekkTilgangTilPersoner(listOf("2", "1", "3")) }
        testWithBrukerContext { service.sjekkTilgangTilPersoner(listOf("3", "3", "3")) }

        // Assert
        assertThat(tilgangPerIdent.all { it.key == it.value.personIdent }).isTrue
        assertThat(tilgangPerIdent.values).containsExactlyInAnyOrderElementsOf(tilganger)
        assertThat(fakeTilgangsmaskinTilgangskontrollKlient.hentKallMotSjekkTilgangTilPersoner())
            .containsExactly(setOf("1"), setOf("2", "3"))
    }

    @Test
    fun `skal ikke cache når tilgangssjekken feiler`() {
        // Arrange
        val tilgangsmaskinTilgangskontrollKlient = mockk<TilgangsmaskinTilgangskontrollKlient>()
        val serviceMedFeilendeKlient =
            PersonTilgangService(
                tilgangsmaskinTilgangskontrollKlient,
                fakeFamilieIntegrasjonerTilgangskontrollKlient,
                featureToggleService,
                cacheManager,
                systemOnlyPdlRestKlient,
            )
        every { tilgangsmaskinTilgangskontrollKlient.sjekkTilgangTilPersoner(setOf("1")) } throws
            Feil("Fikk ikke gyldig svar fra Tilgangsmaskinen for 1 av 1 identer") andThen listOf(PersonTilgang.medTilgang("1"))
        assertThrows<Feil> { testWithBrukerContext { serviceMedFeilendeKlient.sjekkTilgangTilPerson("1") } }

        // Act
        val tilgang = testWithBrukerContext { serviceMedFeilendeKlient.sjekkTilgangTilPerson("1") }

        // Assert
        assertThat(tilgang.harTilgang).isTrue
        verify(exactly = 2) { tilgangsmaskinTilgangskontrollKlient.sjekkTilgangTilPersoner(setOf("1")) }
    }

    @Test
    fun `skal hente identer med strengt fortrolig adressebeskyttelse i Norge og utland`() {
        // Arrange
        val identer = listOf("1", "2", "3", "4", "5")
        every { systemOnlyPdlRestKlient.hentAdressebeskyttelseBolk(identer) } returns
            mapOf(
                "1" to PdlAdressebeskyttelsePerson(listOf(Adressebeskyttelse(ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG))),
                "2" to PdlAdressebeskyttelsePerson(listOf(Adressebeskyttelse(ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG_UTLAND))),
                "3" to PdlAdressebeskyttelsePerson(listOf(Adressebeskyttelse(ADRESSEBESKYTTELSEGRADERING.FORTROLIG))),
                "4" to PdlAdressebeskyttelsePerson(listOf(Adressebeskyttelse(ADRESSEBESKYTTELSEGRADERING.UGRADERT))),
                "5" to PdlAdressebeskyttelsePerson(emptyList()),
            )

        // Act
        val resultat = service.hentIdenterMedStrengtFortroligAdressebeskyttelse(identer)

        // Assert
        assertThat(resultat).containsExactlyInAnyOrder("1", "2")
        verify(exactly = 1) { systemOnlyPdlRestKlient.hentAdressebeskyttelseBolk(identer) }
    }

    @Test
    fun `skal returnere tom liste når ingen har strengt fortrolig adressebeskyttelse`() {
        // Arrange
        val identer = listOf("1", "2")
        every { systemOnlyPdlRestKlient.hentAdressebeskyttelseBolk(identer) } returns
            mapOf(
                "1" to PdlAdressebeskyttelsePerson(listOf(Adressebeskyttelse(ADRESSEBESKYTTELSEGRADERING.FORTROLIG))),
                "2" to PdlAdressebeskyttelsePerson(emptyList()),
            )

        // Act
        val resultat = service.hentIdenterMedStrengtFortroligAdressebeskyttelse(identer)

        // Assert
        assertThat(resultat).isEmpty()
    }
}
