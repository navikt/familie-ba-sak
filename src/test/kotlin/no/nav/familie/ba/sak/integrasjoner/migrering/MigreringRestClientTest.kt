package no.nav.familie.ba.sak.integrasjoner.migrering

import com.github.tomakehurst.wiremock.client.WireMock
import junit.framework.Assert.assertEquals
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTestDev
import no.nav.familie.ba.sak.integrasjoner.infotrygd.domene.MigreringResponseDto
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.client.RestOperations
import java.net.URI

internal class MigreringRestClientTest : AbstractSpringIntegrationTestDev() {
    lateinit var client: MigreringRestClient

    @Autowired
    @Qualifier("jwtBearer")
    lateinit var restOperations: RestOperations

    @BeforeEach
    fun setUp() {
        client = MigreringRestClient(
            URI.create(wireMockServer.baseUrl() + "/api"),
            restOperations
        )
    }

    @AfterEach
    fun clearTest() {
        wireMockServer.resetAll()
    }

    @Test
    fun migrertAvSaksbehandler() {
        wireMockServer.stubFor(
            WireMock.post("/api/migrer/migrert-av-saksbehandler").willReturn(
                WireMock.okJson(
                    objectMapper.writeValueAsString(
                        Ressurs.success("OK")
                    )
                )
            )
        )

        assertEquals(
            client.migrertAvSaksbehandler("123", MigreringResponseDto(fagsakId = 123, behandlingId = 456)),
            "OK"
        )
    }
}
