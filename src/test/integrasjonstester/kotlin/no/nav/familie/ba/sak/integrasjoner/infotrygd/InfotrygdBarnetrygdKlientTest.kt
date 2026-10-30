package no.nav.familie.ba.sak.integrasjoner.infotrygd

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.randomBarnFnr
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.kontrakter.ba.infotrygd.InfotrygdSøkRequest
import no.nav.familie.kontrakter.ba.infotrygd.InfotrygdSøkResponse
import no.nav.familie.kontrakter.ba.infotrygd.Sak
import no.nav.familie.kontrakter.ba.infotrygd.Stønad
import no.nav.familie.kontrakter.felles.jsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.env.Environment
import org.springframework.web.client.RestClient
import java.net.URI

class InfotrygdBarnetrygdKlientTest : AbstractSpringIntegrationTest() {
    val sakerURL = "/api/infotrygd/barnetrygd/saker"
    val stønaderURL = "/api/infotrygd/barnetrygd/stonad?historikk=false"

    private val søkersIdenter = listOf(randomFnr(), randomFnr(), randomFnr())
    private val barnasIdenter = listOf(randomBarnFnr(), randomBarnFnr())

    @Autowired
    @Qualifier("utenAuthRestClient")
    lateinit var restClient: RestClient

    @Autowired
    lateinit var environment: Environment

    lateinit var klient: InfotrygdBarnetrygdKlient

    @BeforeEach
    fun setUp() {
        klient =
            InfotrygdBarnetrygdKlient(
                URI.create(wireMockServer.baseUrl() + "/api"),
                restClient,
                1L,
            )
    }

    @AfterEach
    fun clearTest() {
        wireMockServer.resetAll()
    }

    @Test
    fun `Skal lage InfotrygdBarnetrygdRequest basert på lister med fnr og barns fødselsnummer`() {
        // Arrange
        wireMockServer.stubFor(
            post(sakerURL).willReturn(
                okJson(
                    jsonMapper.writeValueAsString(
                        InfotrygdSøkResponse(listOf(Sak(status = "IP")), emptyList()),
                    ),
                ),
            ),
        )
        wireMockServer.stubFor(
            post(stønaderURL).willReturn(
                okJson(
                    jsonMapper.writeValueAsString(
                        InfotrygdSøkResponse(listOf(Stønad()), emptyList()),
                    ),
                ),
            ),
        )

        val infotrygdSøkRequest = InfotrygdSøkRequest(søkersIdenter, barnasIdenter)

        // Act
        val hentsakerResponse = klient.hentSaker(søkersIdenter, barnasIdenter)
        val hentstønaderResponse = klient.hentStønader(søkersIdenter, barnasIdenter)

        // Assert
        wireMockServer.verify(
            anyRequestedFor(urlEqualTo(sakerURL)).withRequestBody(
                equalToJson(
                    jsonMapper.writeValueAsString(
                        infotrygdSøkRequest,
                    ),
                ),
            ),
        )
        wireMockServer.verify(
            anyRequestedFor(urlEqualTo(stønaderURL)).withRequestBody(
                equalToJson(
                    jsonMapper.writeValueAsString(
                        infotrygdSøkRequest,
                    ),
                ),
            ),
        )
        Assertions.assertEquals(hentsakerResponse.bruker[0].status, "IP")
        Assertions.assertEquals(hentstønaderResponse.bruker.size, 1)
    }

    @Test
    fun `Skal lage InfotrygdBarnetrygdRequest basert på lister med fnr`() {
        // Arrange
        wireMockServer.stubFor(
            post(sakerURL).willReturn(
                okJson(
                    jsonMapper.writeValueAsString(
                        InfotrygdSøkResponse(listOf(Sak(status = "IP")), emptyList()),
                    ),
                ),
            ),
        )

        val infotrygdSøkRequest = InfotrygdSøkRequest(søkersIdenter, emptyList())

        // Act
        val saker = klient.hentSaker(søkersIdenter)

        // Assert
        wireMockServer.verify(
            anyRequestedFor(urlEqualTo(sakerURL)).withRequestBody(
                equalToJson(
                    jsonMapper.writeValueAsString(
                        infotrygdSøkRequest,
                    ),
                ),
            ),
        )
        assertThat(saker.bruker.single().status).isEqualTo("IP")
    }

    @Test
    fun `Invokering av Infotrygd-Barnetrygd genererer http feil`() {
        // Arrange
        wireMockServer.stubFor(post(sakerURL).willReturn(aResponse().withStatus(401)))

        // Act & Assert
        assertThrows<Feil> {
            klient.hentSaker(søkersIdenter, barnasIdenter)
        }
    }
}
