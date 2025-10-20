package no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner

import io.mockk.mockk
import no.nav.familie.ba.sak.common.clearAllCaches
import no.nav.familie.ba.sak.mock.FakeFamilieIntegrasjonerTilgangskontrollClient
import no.nav.familie.ba.sak.util.BrukerContextUtil.testWithBrukerContext
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.web.client.RestTemplate

class FamilieIntegrasjonerTilgangskontrollServiceTest {
    private val fakeFamilieIntegrasjonerTilgangskontrollClient = FakeFamilieIntegrasjonerTilgangskontrollClient(RestTemplate())

    private val cacheManager = ConcurrentMapCacheManager()

    private val service = FamilieIntegrasjonerTilgangskontrollService(fakeFamilieIntegrasjonerTilgangskontrollClient, cacheManager, mockk())

    @BeforeEach
    fun setUp() {
        fakeFamilieIntegrasjonerTilgangskontrollClient.reset()
        cacheManager.clearAllCaches()
    }

    @Test
    fun `har tilgang skal cacheas`() {
        fakeFamilieIntegrasjonerTilgangskontrollClient.leggTilPersonIdentTilTilgang(listOf(Tilgang("1", true)))
        assertThat(testWithBrukerContext { service.sjekkTilgangTilPerson("1") }.harTilgang).isTrue
        fakeFamilieIntegrasjonerTilgangskontrollClient.reset()
        fakeFamilieIntegrasjonerTilgangskontrollClient.leggTilPersonIdentTilTilgang(listOf(Tilgang("1", false)))
        assertThat(testWithBrukerContext { service.sjekkTilgangTilPerson("1") }.harTilgang).isTrue
    }

    @Test
    fun `har ikke tilgang skal caches`() {
        fakeFamilieIntegrasjonerTilgangskontrollClient.leggTilPersonIdentTilTilgang(listOf(Tilgang("1", false)))
        assertThat(testWithBrukerContext { service.sjekkTilgangTilPerson("1") }.harTilgang).isFalse
        fakeFamilieIntegrasjonerTilgangskontrollClient.reset()
        fakeFamilieIntegrasjonerTilgangskontrollClient.leggTilPersonIdentTilTilgang(listOf(Tilgang("1", true)))
        assertThat(testWithBrukerContext { service.sjekkTilgangTilPerson("1") }.harTilgang).isFalse
    }

    @Test
    fun `cacher per saksbehandlere`() {
        fakeFamilieIntegrasjonerTilgangskontrollClient.leggTilPersonIdentTilTilgang(listOf(Tilgang("1", false)))

        // Systemcontext
        service.sjekkTilgangTilPerson("1")
        val kall1 = testWithBrukerContext("saksbehandler1") { service.sjekkTilgangTilPerson("1") }
        fakeFamilieIntegrasjonerTilgangskontrollClient.reset()
        fakeFamilieIntegrasjonerTilgangskontrollClient.leggTilPersonIdentTilTilgang(listOf(Tilgang("1", true)))
        val kall2 = testWithBrukerContext("saksbehandler2") { service.sjekkTilgangTilPerson("1") }
        assertThat(kall1.harTilgang).isFalse
        assertThat(kall2.harTilgang).isTrue
    }

    @Test
    fun `tilgangskontrollerer unike identer`() {
        fakeFamilieIntegrasjonerTilgangskontrollClient.leggTilPersonIdentTilTilgang(listOf(Tilgang("1", false)))

        testWithBrukerContext("saksbehandler1") { service.sjekkTilgangTilPersoner(listOf("1", "1")) }

        assertThat(fakeFamilieIntegrasjonerTilgangskontrollClient.antallKallTilSjekkTilgangTilPersoner()).isEqualTo(1)
    }

    @Test
    fun `skal ikke hente identer som allerede finnes i cachen`() {
        val tilgang = listOf(Tilgang("1", false), Tilgang("2", true), Tilgang("3", false))
        fakeFamilieIntegrasjonerTilgangskontrollClient.leggTilPersonIdentTilTilgang(tilgang)

        testWithBrukerContext { service.sjekkTilgangTilPerson("1") }
        val sjekkTilgangTilPersoner = testWithBrukerContext { service.sjekkTilgangTilPersoner(listOf("2", "1", "3")) }
        testWithBrukerContext { service.sjekkTilgangTilPersoner(listOf("2", "1", "3")) }
        testWithBrukerContext { service.sjekkTilgangTilPersoner(listOf("3", "3", "3")) }

        assertThat(sjekkTilgangTilPersoner.all { it.key == it.value.personIdent })
        assertThat(sjekkTilgangTilPersoner.map { it.key to it.value.harTilgang }).containsExactlyInAnyOrderElementsOf(
            tilgang.map { tilgang -> Pair(tilgang.personIdent, tilgang.harTilgang) }.toList(),
        )

        assertThat(fakeFamilieIntegrasjonerTilgangskontrollClient.antallKallTilSjekkTilgangTilPersoner()).isEqualTo(2)

        val forventetFørsteKall = listOf("1")
        val forventetAndreKall = listOf("2", "3")
        assertThat(fakeFamilieIntegrasjonerTilgangskontrollClient.kallMotSjekkTilgangTilPersoner).containsExactlyElementsOf(listOf(forventetFørsteKall, forventetAndreKall))
    }
}
