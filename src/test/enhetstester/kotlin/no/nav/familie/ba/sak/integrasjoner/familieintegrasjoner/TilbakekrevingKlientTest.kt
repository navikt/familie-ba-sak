package no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.familie.ba.sak.kjerne.klage.KlageKlient
import no.nav.familie.ba.sak.kjerne.tilbakekreving.TilbakekrevingKlient
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestOperations
import java.net.URI
import java.util.UUID

class TilbakekrevingKlientTest {
    private val restOperations: RestOperations = RestTemplateBuilder().messageConverters(MappingJackson2HttpMessageConverter()).build()
    private lateinit var wiremockServerItem: WireMockServer
    private lateinit var tilbakekrevingKlient: TilbakekrevingKlient

    @BeforeEach
    fun initClass() {
        wiremockServerItem = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
        wiremockServerItem.start()
        tilbakekrevingKlient = TilbakekrevingKlient(URI.create(wiremockServerItem.baseUrl()), restOperations)
    }

    @Test
    fun `oppdaterEnhetPåÅpenBehandling - skal sende PUT request med riktig dto og felt til klage`() {
        // Arrange
        wiremockServerItem.stubFor(
            WireMock
                .put(WireMock.urlEqualTo("/baks/portefoljejustering/oppdater-behandlende-enhet"))
                .willReturn(
                    WireMock.okJson(
                        """
                        {
                          "data": "Behandlende enhet oppdatert til 4812",
                          "status": "SUKSESS",
                          "melding": "oppdatert enhet ok",
                          "frontendFeilmelding": null,
                          "stacktrace": null,
                          "callId": null
                        }
                        """.trimIndent(),
                    ),
                ),
        )

        // Act
        val behandlingEksternBrukId = UUID.randomUUID()
        tilbakekrevingKlient.oppdaterEnhetPåÅpenBehandling(behandlingEksternBrukId, "4812")

        // Assert
        wiremockServerItem.verify(
            WireMock
                .putRequestedFor(WireMock.urlEqualTo("/baks/portefoljejustering/oppdater-behandlende-enhet"))
                .withRequestBody(WireMock.matchingJsonPath("$.behandlingEksternBrukId", WireMock.equalTo(behandlingEksternBrukId.toString())))
                .withRequestBody(WireMock.matchingJsonPath("$.nyEnhet", WireMock.equalTo("4812"))),
        )
    }
}
