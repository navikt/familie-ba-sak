package no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.BarnetrygdEnhet
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.NavIdent
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.dokarkiv.AvsluttSakRequest
import no.nav.familie.kontrakter.felles.dokarkiv.DokarkivBruker
import no.nav.familie.kontrakter.felles.dokarkiv.GjenåpneSakRequest
import no.nav.familie.kontrakter.felles.journalpost.Bruker
import no.nav.familie.kontrakter.felles.journalpost.JournalposterForBrukerRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime

class IntegrasjonKlientTest {
    private lateinit var wiremockServerItem: WireMockServer
    private lateinit var integrasjonKlient: IntegrasjonKlient
    private lateinit var systemOnlyIntegrasjonKlient: SystemOnlyIntegrasjonKlient
    private val featureToggleService = mockk<FeatureToggleService>()

    @BeforeEach
    fun initClass() {
        wiremockServerItem = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
        wiremockServerItem.start()
        every { featureToggleService.isEnabled(FeatureToggle.BRUK_FAMILIE_INTEGRASJONER_I_GCP, defaultValue = false) } returns false
        val fssUri = URI.create(wiremockServerItem.baseUrl())
        val gcpUri = URI.create("${wiremockServerItem.baseUrl()}/gcp")
        val restClientFss = RestClient.builder().defaultHeader("X-Test-Cluster", "fss").build()
        val restClientGcp = RestClient.builder().defaultHeader("X-Test-Cluster", "gcp").build()
        integrasjonKlient = IntegrasjonKlient(fssUri, restClientFss, gcpUri, restClientGcp, featureToggleService, 1)
        systemOnlyIntegrasjonKlient = SystemOnlyIntegrasjonKlient(fssUri, restClientFss, gcpUri, restClientGcp, featureToggleService, 1)
    }

    @AfterEach
    fun tearDown() {
        wiremockServerItem.stop()
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `skal velge URL og klient fra samme cluster ut fra togglen`(brukGcp: Boolean) {
        // Arrange
        every { featureToggleService.isEnabled(FeatureToggle.BRUK_FAMILIE_INTEGRASJONER_I_GCP, defaultValue = false) } returns brukGcp
        val sti = if (brukGcp) "/gcp/kodeverk/landkoder/NOR" else "/kodeverk/landkoder/NOR"
        val cluster = if (brukGcp) "gcp" else "fss"
        wiremockServerItem.stubFor(
            WireMock
                .get(WireMock.urlEqualTo(sti))
                .withHeader("X-Test-Cluster", WireMock.equalTo(cluster))
                .willReturn(WireMock.okJson("""{"data":"Norge","status":"SUKSESS","melding":"OK"}""")),
        )

        // Act
        val land = integrasjonKlient.hentLand("NOR")

        // Assert
        assertThat(land).isEqualTo("Norge")
        wiremockServerItem.verify(WireMock.getRequestedFor(WireMock.urlEqualTo(sti)).withHeader("X-Test-Cluster", WireMock.equalTo(cluster)))
        verify(exactly = 1) { featureToggleService.isEnabled(FeatureToggle.BRUK_FAMILIE_INTEGRASJONER_I_GCP, defaultValue = false) }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `skal velge URL og systemklient fra samme cluster ut fra togglen`(brukGcp: Boolean) {
        // Arrange
        every { featureToggleService.isEnabled(FeatureToggle.BRUK_FAMILIE_INTEGRASJONER_I_GCP, defaultValue = false) } returns brukGcp
        val sti = if (brukGcp) "/gcp/aareg/arbeidsforhold" else "/aareg/arbeidsforhold"
        val cluster = if (brukGcp) "gcp" else "fss"
        wiremockServerItem.stubFor(
            WireMock
                .post(WireMock.urlEqualTo(sti))
                .withHeader("X-Test-Cluster", WireMock.equalTo(cluster))
                .willReturn(WireMock.okJson("""{"data":[],"status":"SUKSESS","melding":"OK"}""")),
        )

        // Act
        val arbeidsforhold = systemOnlyIntegrasjonKlient.hentArbeidsforholdMedSystembruker("test-ident", LocalDate.of(2026, 1, 1))

        // Assert
        assertThat(arbeidsforhold).isEmpty()
        wiremockServerItem.verify(WireMock.postRequestedFor(WireMock.urlEqualTo(sti)).withHeader("X-Test-Cluster", WireMock.equalTo(cluster)))
        verify(exactly = 1) { featureToggleService.isEnabled(FeatureToggle.BRUK_FAMILIE_INTEGRASJONER_I_GCP, defaultValue = false) }
    }

    @Test
    fun `skal kunne bytte til GCP og tilbake til FSS uten omstart`() {
        // Arrange
        every {
            featureToggleService.isEnabled(FeatureToggle.BRUK_FAMILIE_INTEGRASJONER_I_GCP, defaultValue = false)
        } returnsMany listOf(false, true, false)
        listOf("/kodeverk/landkoder/NOR", "/gcp/kodeverk/landkoder/NOR").forEach { sti ->
            wiremockServerItem.stubFor(
                WireMock.get(WireMock.urlEqualTo(sti)).willReturn(WireMock.okJson("""{"data":"Norge","status":"SUKSESS","melding":"OK"}""")),
            )
        }

        // Act
        repeat(3) { integrasjonKlient.hentLand("NOR") }

        // Assert
        wiremockServerItem.verify(2, WireMock.getRequestedFor(WireMock.urlEqualTo("/kodeverk/landkoder/NOR")).withHeader("X-Test-Cluster", WireMock.equalTo("fss")))
        wiremockServerItem.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/gcp/kodeverk/landkoder/NOR")).withHeader("X-Test-Cluster", WireMock.equalTo("gcp")))
        verify(exactly = 3) { featureToggleService.isEnabled(FeatureToggle.BRUK_FAMILIE_INTEGRASJONER_I_GCP, defaultValue = false) }
    }

    @Test
    fun `skal ikke falle tilbake til FSS når et kall til GCP feiler`() {
        // Arrange
        every { featureToggleService.isEnabled(FeatureToggle.BRUK_FAMILIE_INTEGRASJONER_I_GCP, defaultValue = false) } returns true
        wiremockServerItem.stubFor(WireMock.get(WireMock.urlEqualTo("/gcp/kodeverk/landkoder/NOR")).willReturn(WireMock.serverError()))

        // Act & Assert
        assertThrows<IntegrasjonException> { integrasjonKlient.hentLand("NOR") }
        wiremockServerItem.verify(0, WireMock.getRequestedFor(WireMock.urlEqualTo("/kodeverk/landkoder/NOR")))
        wiremockServerItem.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/gcp/kodeverk/landkoder/NOR")))
    }

    @Test
    fun `avsluttSak - skal avslutte sak i dokarkiv`() {
        // Arrange
        wiremockServerItem.stubFor(
            WireMock
                .patch(WireMock.urlEqualTo("/arkiv/avsluttSak"))
                .willReturn(WireMock.okJson("""{"data":null,"status":"SUKSESS","melding":"OK"}""")),
        )

        // Act
        integrasjonKlient.avsluttSak(lagAvsluttSakRequest())

        // Assert
        wiremockServerItem.verify(WireMock.patchRequestedFor(WireMock.urlEqualTo("/arkiv/avsluttSak")))
    }

    @Test
    fun `avsluttSak - skal ikke kaste feil når dokarkiv svarer 404 fordi fagsaken ikke har noen arkivsak`() {
        // Arrange
        wiremockServerItem.stubFor(
            WireMock
                .patch(WireMock.urlEqualTo("/arkiv/avsluttSak"))
                .willReturn(
                    WireMock
                        .aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"data":null,"status":"FEILET","melding":"[dokarkiv.avsluttSak][Feil ved avslutting av sak i dokarkiv av journalpost ][org.springframework.web.client.HttpClientErrorException${'$'}NotFound]"}"""),
                ),
        )

        // Act & Assert
        assertDoesNotThrow { integrasjonKlient.avsluttSak(lagAvsluttSakRequest()) }
    }

    @Test
    fun `avsluttSak - skal kaste feil når 404 ikke kommer fra dokarkiv`() {
        // Arrange
        wiremockServerItem.stubFor(
            WireMock
                .patch(WireMock.urlEqualTo("/arkiv/avsluttSak"))
                .willReturn(WireMock.aResponse().withStatus(404)),
        )

        // Act & Assert
        assertThrows<HttpClientErrorException.NotFound> { integrasjonKlient.avsluttSak(lagAvsluttSakRequest()) }
    }

    @Test
    fun `gjenåpneSakIDokarkiv - skal gjenåpne sak i dokarkiv`() {
        // Arrange
        wiremockServerItem.stubFor(
            WireMock
                .patch(WireMock.urlEqualTo("/arkiv/gjenaapneSak"))
                .willReturn(WireMock.okJson("""{"data":null,"status":"SUKSESS","melding":"OK"}""")),
        )

        // Act
        integrasjonKlient.gjenåpneSakIDokarkiv(lagGjenåpneSakRequest())

        // Assert
        wiremockServerItem.verify(WireMock.patchRequestedFor(WireMock.urlEqualTo("/arkiv/gjenaapneSak")))
    }

    @Test
    fun `gjenåpneSakIDokarkiv - skal ikke kaste feil når dokarkiv svarer 404 fordi det ikke finnes noen avsluttet arkivsak`() {
        // Arrange
        wiremockServerItem.stubFor(
            WireMock
                .patch(WireMock.urlEqualTo("/arkiv/gjenaapneSak"))
                .willReturn(
                    WireMock
                        .aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"data":null,"status":"FEILET","melding":"[dokarkiv.gjenaapneSak][Feil ved gjenåpning av sak i dokarkiv av journalpost ][org.springframework.web.client.HttpClientErrorException${'$'}NotFound]"}"""),
                ),
        )

        // Act & Assert
        assertDoesNotThrow { integrasjonKlient.gjenåpneSakIDokarkiv(lagGjenåpneSakRequest()) }
    }

    @Test
    fun `gjenåpneSakIDokarkiv - skal kaste feil når 404 ikke kommer fra dokarkiv`() {
        // Arrange
        wiremockServerItem.stubFor(
            WireMock
                .patch(WireMock.urlEqualTo("/arkiv/gjenaapneSak"))
                .willReturn(WireMock.aResponse().withStatus(404)),
        )

        // Act & Assert
        assertThrows<HttpClientErrorException.NotFound> { integrasjonKlient.gjenåpneSakIDokarkiv(lagGjenåpneSakRequest()) }
    }

    private fun lagGjenåpneSakRequest() =
        GjenåpneSakRequest(
            tema = Tema.BAR,
            fagsakId = "1",
            fagsaksystem = Fagsystem.BA,
            bruker = DokarkivBruker(BrukerIdType.FNR, "12345678910"),
        )

    private fun lagAvsluttSakRequest() =
        AvsluttSakRequest(
            tema = Tema.BAR,
            fagsakId = "1",
            fagsaksystem = Fagsystem.BA,
            bruker = DokarkivBruker(BrukerIdType.FNR, "12345678910"),
            opprettetDato = LocalDateTime.now().minusYears(20),
            avsluttetDato = LocalDateTime.now(),
            administrativEnhet = "4820",
        )

    @Test
    fun `hentEnheterSomNavIdentHarTilgangTil - skal hente enheter som NAV-ident har tilgang til`() {
        // Arrange
        val navIdent = NavIdent("1")

        wiremockServerItem.stubFor(
            WireMock
                .get(WireMock.urlEqualTo("/saksbehandler/1/grupper"))
                .willReturn(WireMock.okJson(readFile("enheterNavIdentHarTilgangTilResponse.json"))),
        )

        // Act
        val enheter = integrasjonKlient.hentBehandlendeEnheterSomNavIdentHarTilgangTil(navIdent)

        // Assert
        assertThat(enheter).hasSize(2)
        assertThat(enheter).anySatisfy {
            assertThat(it.enhetsnummer).isEqualTo(BarnetrygdEnhet.VADSØ.enhetsnummer)
            assertThat(it.enhetsnavn).isEqualTo(BarnetrygdEnhet.VADSØ.enhetsnavn)
        }
        assertThat(enheter).anySatisfy {
            assertThat(it.enhetsnummer).isEqualTo(BarnetrygdEnhet.VIKAFOSSEN.enhetsnummer)
            assertThat(it.enhetsnavn).isEqualTo(BarnetrygdEnhet.VIKAFOSSEN.enhetsnavn)
        }
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
        val tilgangsstyrteJournalposter = integrasjonKlient.hentTilgangsstyrteJournalposterForBruker(JournalposterForBrukerRequest(brukerId = Bruker(id = "12345678910", type = BrukerIdType.FNR), antall = 100, tema = listOf(Tema.BAR)))

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
