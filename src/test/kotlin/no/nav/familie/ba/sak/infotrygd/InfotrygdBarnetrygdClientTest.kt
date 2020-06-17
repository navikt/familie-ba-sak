package no.nav.familie.ba.sak.infotrygd

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.*
import no.nav.commons.foedselsnummer.FoedselsNr
import no.nav.familie.ba.sak.config.ApplicationConfig
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.kontrakter.felles.objectMapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.core.env.Environment
import org.springframework.core.env.get
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.client.RestOperations
import java.net.URI

@SpringBootTest(classes = [ApplicationConfig::class])
@ActiveProfiles("dev", "integrasjonstest", "mock-oauth")
@AutoConfigureWireMock(port = 0)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InfotrygdBarnetrygdClientTest {

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
                URI.create("http://localhost:${environment.get("wiremock.server.port")}/api"),
                restOperations
        )
    }

    @Test
    fun `Skal lage InfotrygdBarnetrygdRequest basert på lister med fnr`() {
        stubFor(WireMock.post("/api/infotrygd/barnetrygd/personsok").willReturn(okJson(objectMapper.writeValueAsString(
                InfotrygdSøkResponse(true)))))

        val søkersIdenter = ClientMocks.søkerFnr.toList()
        val barnasIdenter = ClientMocks.barnFnr.toList()
        val infotrygdSøkRequest = InfotrygdSøkRequest(søkersIdenter.map {
            FoedselsNr(it)
        }, barnasIdenter.map { FoedselsNr(it) })

        val finnesIkkeHosInfotrygd = client.finnesIkkeHosInfotrygd(søkersIdenter, barnasIdenter)

        verify(anyRequestedFor(anyUrl()).withRequestBody(equalToJson(objectMapper.writeValueAsString (infotrygdSøkRequest))))
        Assertions.assertEquals(true, finnesIkkeHosInfotrygd)
    }


}