package no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.familie.ba.sak.kjerne.klage.KlageKlient
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestOperations
import java.net.URI

class KlageKlientTest {
    private val restOperations: RestOperations = RestTemplateBuilder().messageConverters(MappingJackson2HttpMessageConverter()).build()
    private lateinit var wiremockServerItem: WireMockServer
    private lateinit var klageKlient: KlageKlient

    @BeforeEach
    fun initClass() {
        wiremockServerItem = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
        wiremockServerItem.start()
        klageKlient = KlageKlient(restOperations, URI.create(wiremockServerItem.baseUrl()))
    }

    @Test
    fun `oppdaterEnhetPåÅpenBehandling - skal sende PUT request med riktig dto og felt til klage`() {
        // Arrange
        wiremockServerItem.stubFor(
            WireMock
                .put(WireMock.urlEqualTo("/api/portefoljejustering/oppdater-behandlende-enhet"))
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
        klageKlient.oppdaterEnhetPåÅpenBehandling(1, "4812")

        // Assert
        wiremockServerItem.verify(
            WireMock
                .putRequestedFor(WireMock.urlEqualTo("/api/portefoljejustering/oppdater-behandlende-enhet"))
                .withRequestBody(WireMock.matchingJsonPath("$.oppgaveId", WireMock.equalTo("1")))
                .withRequestBody(WireMock.matchingJsonPath("$.nyEnhet", WireMock.equalTo("4812")))
                .withRequestBody(WireMock.matchingJsonPath("$.fagsystem", WireMock.equalTo("BA"))),
        )
    }
}
