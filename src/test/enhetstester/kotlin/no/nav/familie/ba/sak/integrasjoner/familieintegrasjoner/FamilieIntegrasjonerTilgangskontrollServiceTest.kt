package no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.util.BrukerContextUtil.testWithBrukerContext
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.cache.concurrent.ConcurrentMapCacheManager

class FamilieIntegrasjonerTilgangskontrollServiceTest {

    private val client = mockk<FamilieIntegrasjonerTilgangskontrollClient>()

    private val cacheManager = ConcurrentMapCacheManager()

    private val service = FamilieIntegrasjonerTilgangskontrollService(client, cacheManager, mockk())

    private val slot = mutableListOf<List<String>>()

    @BeforeEach
    fun setUp() {
        slot.clear()
        cacheManager.cacheNames.forEach { cacheManager.getCache(it)?.clear() }
    }

    @Test
    fun `har tilgang skal cacheas`() {
        mockSjekkTilgang(true)

        assertThat(testWithBrukerContext { service.sjekkTilgangTilPerson("1") }.harTilgang).isTrue
        assertThat(testWithBrukerContext { service.sjekkTilgangTilPerson("1") }.harTilgang).isTrue
        verify(exactly = 1) { client.sjekkTilgangTilPersoner(any()) }
    }

    @Test
    fun `har ikke tilgang skal cacheas`() {
        mockSjekkTilgang(false)

        assertThat(testWithBrukerContext { service.sjekkTilgangTilPerson("1") }.harTilgang).isFalse
        assertThat(testWithBrukerContext { service.sjekkTilgangTilPerson("1") }.harTilgang).isFalse
        verify(exactly = 1) { client.sjekkTilgangTilPersoner(any()) }
    }

    @Test
    fun `cachear per saksbehandlere`() {
        mockSjekkTilgang(false)

        val kall1 = testWithBrukerContext("saksbehandler1") { service.sjekkTilgangTilPerson("1") }
        val kall2 = testWithBrukerContext("saksbehandler2") { service.sjekkTilgangTilPerson("1") }
        assertThat(kall1.harTilgang).isFalse
        assertThat(kall2.harTilgang).isFalse
        verify(exactly = 2) { client.sjekkTilgangTilPersoner(any()) }
    }

    @Test
    fun `tilgangskontrollerer unike identer`() {
        mockSjekkTilgang(false)

        testWithBrukerContext("saksbehandler1") { service.sjekkTilgangTilPersoner(listOf("1", "1")) }

        verify(exactly = 1) { client.sjekkTilgangTilPersoner(listOf("1")) }
    }

    @Test
    fun `skal ikke hente identer som allerede finnes i cachen`() {
        val tilgang = mapOf("1" to false, "2" to true, "3" to false)
        mockSjekkTilgang(tilgang)

        testWithBrukerContext { service.sjekkTilgangTilPerson("1") }
        val sjekkTilgangTilPersoner = testWithBrukerContext { service.sjekkTilgangTilPersoner(listOf("2", "1", "3")) }
        testWithBrukerContext { service.sjekkTilgangTilPersoner(listOf("2", "1", "3")) }
        testWithBrukerContext { service.sjekkTilgangTilPersoner(listOf("3", "3", "3")) }

        assertThat(sjekkTilgangTilPersoner.all { it.key == it.value.personIdent })
        assertThat(sjekkTilgangTilPersoner.map { it.key to it.value.harTilgang }).containsExactlyInAnyOrderElementsOf(
            tilgang.entries.map { Pair(it.key, it.value) }.toList(),
        )

        verify(exactly = 2) { client.sjekkTilgangTilPersoner(any()) }

        val forventetFørsteKall = listOf("1")
        val forventetAndreKall = listOf("2", "3")
        assertThat(slot).containsExactlyElementsOf(listOf(forventetFørsteKall, forventetAndreKall))
    }

    private fun mockSjekkTilgang(map: Map<String, Boolean>) {
        every { client.sjekkTilgangTilPersoner(capture(slot)) } answers {
            val arg = firstArg<List<String>>()
            map.entries.filter { arg.contains(it.key) }.map { Tilgang(personIdent = it.key, harTilgang = it.value) }
        }
    }

    private fun mockSjekkTilgang(harTilgang: Boolean = false) {
        every { client.sjekkTilgangTilPersoner(capture(slot)) } answers {
            firstArg<List<String>>().map { Tilgang(personIdent = it, harTilgang = harTilgang) }
        }
    }
}
