package no.nav.familie.ba.sak.infotrygd

import com.github.tomakehurst.wiremock.client.WireMock.*
import no.nav.familie.ba.sak.config.ApplicationConfig
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.kontrakter.ba.infotrygd.InfotrygdSøkRequest
import no.nav.familie.kontrakter.ba.infotrygd.InfotrygdSøkResponse
import no.nav.familie.kontrakter.ba.infotrygd.Sak
import no.nav.familie.kontrakter.ba.infotrygd.Stønad
import no.nav.familie.kontrakter.felles.objectMapper
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.core.env.Environment
import org.springframework.core.env.get
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestOperations
import java.net.URI

@SpringBootTest(classes = [ApplicationConfig::class])
@ActiveProfiles("dev", "integrasjonstest", "mock-oauth")
@AutoConfigureWireMock(port = 0)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("integration")
class InfotrygdBarnetrygdClientTest {
    val løpendeBarnetrygdURL = "/api/infotrygd/barnetrygd/lopende-barnetrygd"
    val sakerURL = "/api/infotrygd/barnetrygd/saker"
    val stønaderURL =  "/api/infotrygd/barnetrygd/stonad"

    @Autowired
    @Qualifier("jwtBearer")
    lateinit var restOperations: RestOperations

    @Autowired
    lateinit var environment: Environment

    lateinit var client: InfotrygdBarnetrygdClient

    @BeforeEach
    fun setUp() {
        resetAllRequests()
        client = InfotrygdBarnetrygdClient(
                URI.create("http://localhost:${environment["wiremock.server.port"]}/api"),
                restOperations,
                environment
        )
    }


    @Test
    fun `Skal lage InfotrygdBarnetrygdRequest basert på lister med fnr og barns fødselsnummer`() {
        stubFor(post(løpendeBarnetrygdURL).willReturn(okJson(objectMapper.writeValueAsString(
                InfotrygdLøpendeBarnetrygdResponse(false)))))
        stubFor(post(sakerURL).willReturn(okJson(objectMapper.writeValueAsString(
                InfotrygdSøkResponse(listOf(Sak(status = "IP")), emptyList())))))
        stubFor(post(stønaderURL).willReturn(okJson(objectMapper.writeValueAsString(
                InfotrygdSøkResponse(listOf(Stønad()), emptyList())))))

        val søkersIdenter = ClientMocks.søkerFnr.toList()
        val barnasIdenter = ClientMocks.barnFnr.toList()
        val infotrygdSøkRequest = InfotrygdSøkRequest(søkersIdenter, barnasIdenter)

        val finnesIkkeHosInfotrygd = client.harLøpendeSakIInfotrygd(søkersIdenter, barnasIdenter)
        val hentsakerResponse = client.hentSaker(søkersIdenter, barnasIdenter)
        val hentstønaderResponse = client.hentStønader(søkersIdenter, barnasIdenter)

        verify(anyRequestedFor(urlEqualTo(løpendeBarnetrygdURL)).withRequestBody(equalToJson(objectMapper.writeValueAsString(
                infotrygdSøkRequest))))
        verify(anyRequestedFor(urlEqualTo(sakerURL)).withRequestBody(equalToJson(objectMapper.writeValueAsString(
                infotrygdSøkRequest))))
        verify(anyRequestedFor(urlEqualTo(stønaderURL)).withRequestBody(equalToJson(objectMapper.writeValueAsString(
                infotrygdSøkRequest))))
        Assertions.assertEquals(false, finnesIkkeHosInfotrygd)
        Assertions.assertEquals(hentsakerResponse.bruker[0].status, "IP")
        Assertions.assertEquals(hentstønaderResponse.bruker.size, 1)
    }


    @Test
    fun `Skal lage InfotrygdBarnetrygdRequest basert på lister med fnr`() {
        stubFor(post(løpendeBarnetrygdURL).willReturn(okJson(objectMapper.writeValueAsString(
                InfotrygdLøpendeBarnetrygdResponse(false)))))

        val søkersIdenter = ClientMocks.søkerFnr.toList()
        val infotrygdSøkRequest = InfotrygdSøkRequest(søkersIdenter, emptyList())

        val finnesIkkeHosInfotrygd = client.harLøpendeSakIInfotrygd(søkersIdenter, emptyList())

        verify(anyRequestedFor(urlEqualTo(løpendeBarnetrygdURL)).withRequestBody(equalToJson(objectMapper.writeValueAsString(
                infotrygdSøkRequest))))
        Assertions.assertEquals(false, finnesIkkeHosInfotrygd)
    }

    @Test
    fun `Invokering av Infotrygd-Barnetrygd genererer http feil`() {
        stubFor(post(løpendeBarnetrygdURL).willReturn(aResponse().withStatus(401)))

        assertThrows<HttpClientErrorException> {
            client.harLøpendeSakIInfotrygd(ClientMocks.søkerFnr.toList(), ClientMocks.barnFnr.toList())
        }
    }
}