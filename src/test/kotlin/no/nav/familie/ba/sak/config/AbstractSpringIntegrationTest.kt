package no.nav.familie.ba.sak.config

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.integrasjoner.`ef-sak`.EfSakRestClient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Tag
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@SpringBootTest
@ActiveProfiles(
    "postgres",
    "mock-økonomi",
    "mock-pdl",
    "mock-task-repository",
    "mock-infotrygd-barnetrygd",
    "mock-tilbakekreving-klient",
    "mock-brev-klient",
    "mock-infotrygd-feed",
    "mock-oauth",
    "mock-rest-template-config",
)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@Tag("integration")
abstract class AbstractSpringIntegrationTest(
    personopplysningerService: PersonopplysningerService? = null,
    personidentService: PersonidentService? = null,
    integrasjonClient: IntegrasjonClient? = null,
    efSakRestClient: EfSakRestClient? = null,
) : AbstractMockkSpringRunner(personopplysningerService, personidentService, integrasjonClient, efSakRestClient) {
    protected final val wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())

    init {
        wireMockServer.start()
    }

    @AfterAll
    fun stopWiremockServer() {
        wireMockServer.stop()
    }
}
