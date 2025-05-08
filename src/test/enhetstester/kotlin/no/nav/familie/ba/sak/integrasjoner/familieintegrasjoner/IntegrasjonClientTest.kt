package no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.mockk.mockk
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.BarnetrygdEnhet
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.NavIdent
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.journalpost.Bruker
import no.nav.familie.kontrakter.felles.journalpost.JournalposterForBrukerRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.web.client.RestOperations
import java.net.URI

class IntegrasjonClientTest {
    private val mockedRestOperations: RestOperations = mockk()
    private val restOperations: RestOperations = RestTemplateBuilder().build()
    private val baseUri = URI("http://localhost:8080")
    private lateinit var wiremockServerItem: WireMockServer
    private lateinit var integrasjonClient: IntegrasjonClient

    @BeforeEach
    fun initClass() {
        wiremockServerItem = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
        wiremockServerItem.start()
        integrasjonClient = IntegrasjonClient(URI.create(wiremockServerItem.baseUrl()), restOperations)
    }

    @Test
    fun `hentTilgangsstyrteJournalposterForBruker - skal hente tilgangsstyrte journalposter for bruker`() {
        // Arrange
        wiremockServerItem.stubFor(
            WireMock
                .post(WireMock.urlEqualTo("/journalpost/tilgangsstyrt/baks"))
                .willReturn(WireMock.okJson(readFile("hentTilgangsstyrteJournalposterForBruker.json"))),
        )

        // Act
        val tilgangsstyrteJournalposter = integrasjonClient.hentTilgangsstyrteJournalposterForBruker(JournalposterForBrukerRequest(brukerId = Bruker(id = "12345678910", type = BrukerIdType.FNR), antall = 100, tema = listOf(Tema.BAR)))

        // Assert
        assertThat(tilgangsstyrteJournalposter).hasSize(1)
        val tilgangsstyrtJournalpost = tilgangsstyrteJournalposter.single()
        assertThat(tilgangsstyrtJournalpost.journalpost.journalpostId).isEqualTo("453492634")
        assertThat(tilgangsstyrtJournalpost.journalpost.tema).isEqualTo(Tema.BAR.name)
        assertThat(tilgangsstyrtJournalpost.journalpost.kanal).isEqualTo("NAV_NO")
        assertThat(tilgangsstyrtJournalpost.journalpostTilgang.harTilgang).isTrue
    }

    private fun readFile(filnavn: String): String = this::class.java.getResource("/familieintegrasjoner/json/$filnavn").readText()
}
