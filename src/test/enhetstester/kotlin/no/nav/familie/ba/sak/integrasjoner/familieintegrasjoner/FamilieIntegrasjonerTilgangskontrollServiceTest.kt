package no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.clearAllCaches
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestKlient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlAdressebeskyttelsePerson
import no.nav.familie.ba.sak.mock.FakeFamilieIntegrasjonerTilgangskontrollKlient
import no.nav.familie.ba.sak.util.BrukerContextUtil.testWithBrukerContext
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.Adressebeskyttelse
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.cache.concurrent.ConcurrentMapCacheManager

class FamilieIntegrasjonerTilgangskontrollServiceTest {
    private val fakeFamilieIntegrasjonerTilgangskontrollKlient = FakeFamilieIntegrasjonerTilgangskontrollKlient()

    private val cacheManager = ConcurrentMapCacheManager()

    private val systemOnlyPdlRestKlient = mockk<SystemOnlyPdlRestKlient>()

    private val service =
        FamilieIntegrasjonerTilgangskontrollService(
            fakeFamilieIntegrasjonerTilgangskontrollKlient,
            cacheManager,
            systemOnlyPdlRestKlient,
            mockk(relaxed = true),
        )

    @BeforeEach
    fun setUp() {
        cacheManager.clearAllCaches()
    }

    @AfterEach
    fun tearDown() {
        fakeFamilieIntegrasjonerTilgangskontrollKlient.reset()
    }

    @Test
    fun `har tilgang skal cacheas`() {
        // Arrange
        fakeFamilieIntegrasjonerTilgangskontrollKlient.leggTilTilganger(listOf(Tilgang("1", true)))

        // Act & Assert
        assertThat(testWithBrukerContext { service.sjekkTilgangTilPerson("1") }.harTilgang).isTrue
        fakeFamilieIntegrasjonerTilgangskontrollKlient.reset()
        fakeFamilieIntegrasjonerTilgangskontrollKlient.leggTilTilganger(listOf(Tilgang("1", false)))
        assertThat(testWithBrukerContext { service.sjekkTilgangTilPerson("1") }.harTilgang).isTrue
    }

    @Test
    fun `har ikke tilgang skal caches`() {
        // Arrange
        fakeFamilieIntegrasjonerTilgangskontrollKlient.leggTilTilganger(listOf(Tilgang("1", false)))

        // Act & Assert
        assertThat(testWithBrukerContext { service.sjekkTilgangTilPerson("1") }.harTilgang).isFalse
        fakeFamilieIntegrasjonerTilgangskontrollKlient.reset()
        fakeFamilieIntegrasjonerTilgangskontrollKlient.leggTilTilganger(listOf(Tilgang("1", true)))
        assertThat(testWithBrukerContext { service.sjekkTilgangTilPerson("1") }.harTilgang).isFalse
    }

    @Test
    fun `cacher per saksbehandlere`() {
        // Arrange
        fakeFamilieIntegrasjonerTilgangskontrollKlient.leggTilTilganger(listOf(Tilgang("1", false)))

        // Act
        // Systemcontext
        service.sjekkTilgangTilPerson("1")
        val kall1 = testWithBrukerContext("saksbehandler1") { service.sjekkTilgangTilPerson("1") }
        fakeFamilieIntegrasjonerTilgangskontrollKlient.reset()
        fakeFamilieIntegrasjonerTilgangskontrollKlient.leggTilTilganger(listOf(Tilgang("1", true)))
        val kall2 = testWithBrukerContext("saksbehandler2") { service.sjekkTilgangTilPerson("1") }

        // Assert
        assertThat(kall1.harTilgang).isFalse
        assertThat(kall2.harTilgang).isTrue
    }

    @Test
    fun `tilgangskontrollerer unike identer`() {
        // Arrange
        fakeFamilieIntegrasjonerTilgangskontrollKlient.leggTilTilganger(listOf(Tilgang("1", false)))

        // Act
        testWithBrukerContext("saksbehandler1") { service.sjekkTilgangTilPersoner(listOf("1", "1")) }

        // Assert
        assertThat(fakeFamilieIntegrasjonerTilgangskontrollKlient.antallKallTilSjekkTilgangTilPersoner()).isEqualTo(1)
    }

    @Test
    fun `skal ikke hente identer som allerede finnes i cachen`() {
        // Arrange
        val tilgang = listOf(Tilgang("1", false), Tilgang("2", true), Tilgang("3", false))
        fakeFamilieIntegrasjonerTilgangskontrollKlient.leggTilTilganger(tilgang)

        // Act
        testWithBrukerContext { service.sjekkTilgangTilPerson("1") }
        val sjekkTilgangTilPersoner = testWithBrukerContext { service.sjekkTilgangTilPersoner(listOf("2", "1", "3")) }
        testWithBrukerContext { service.sjekkTilgangTilPersoner(listOf("2", "1", "3")) }
        testWithBrukerContext { service.sjekkTilgangTilPersoner(listOf("3", "3", "3")) }

        // Assert
        assertThat(sjekkTilgangTilPersoner.all { it.key == it.value.personIdent })
        assertThat(sjekkTilgangTilPersoner.map { it.key to it.value.harTilgang }).containsExactlyInAnyOrderElementsOf(
            tilgang.map { tilgang -> Pair(tilgang.personIdent, tilgang.harTilgang) }.toList(),
        )

        val faktiskeKall = fakeFamilieIntegrasjonerTilgangskontrollKlient.hentKallMotSjekkTilgangTilPersoner()
        assertThat(faktiskeKall).hasSize(2)

        val forventetFørsteKall = listOf("1")
        val forventetAndreKall = listOf("2", "3")
        assertThat(faktiskeKall).containsExactlyElementsOf(listOf(forventetFørsteKall, forventetAndreKall))
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
