package no.nav.familie.ba.sak.mock

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.fake.BARN_DET_IKKE_GIS_TILGANG_TIL_FNR
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollKlient
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@TestConfiguration
@Profile("dev", "postgres")
class FamilieIntegrasjonerTilgangskontrollMock {
    @Bean
    @Primary
    fun mockFamilieIntegrasjonerTilgangskontrollKlient(): FamilieIntegrasjonerTilgangskontrollKlient {
        val mockFamilieIntegrasjonerTilgangskontrollKlient =
            mockk<FamilieIntegrasjonerTilgangskontrollKlient>(relaxed = false)

        clearMockFamilieIntegrasjonerTilgangskontrollKlient(mockFamilieIntegrasjonerTilgangskontrollKlient)

        return mockFamilieIntegrasjonerTilgangskontrollKlient
    }

    companion object {
        fun clearMockFamilieIntegrasjonerTilgangskontrollKlient(mockFamilieIntegrasjonerTilgangskontrollKlient: FamilieIntegrasjonerTilgangskontrollKlient) {
            clearMocks(mockFamilieIntegrasjonerTilgangskontrollKlient)

            every {
                mockFamilieIntegrasjonerTilgangskontrollKlient.sjekkTilgangTilPersoner(any())
            } answers {
                val identer = firstArg<List<String>>()
                identer.map { Tilgang(personIdent = it, harTilgang = it != BARN_DET_IKKE_GIS_TILGANG_TIL_FNR) }
            }
        }

        fun FamilieIntegrasjonerTilgangskontrollKlient.mockSjekkTilgang(
            map: Map<String, Boolean>,
            slot: MutableList<List<String>> = mutableListOf(),
        ) {
            every { sjekkTilgangTilPersoner(capture(slot)) } answers {
                val arg = firstArg<List<String>>()
                map.entries.filter { arg.contains(it.key) }.map { Tilgang(personIdent = it.key, harTilgang = it.value) }
            }
        }

        fun FamilieIntegrasjonerTilgangskontrollKlient.mockSjekkTilgang(
            harTilgang: Boolean = false,
            slot: MutableList<List<String>> = mutableListOf(),
            begrunnelse: String? = null,
        ) {
            every { sjekkTilgangTilPersoner(capture(slot)) } answers {
                firstArg<List<String>>().map { Tilgang(personIdent = it, harTilgang = harTilgang, begrunnelse = begrunnelse) }
            }
        }
    }
}
