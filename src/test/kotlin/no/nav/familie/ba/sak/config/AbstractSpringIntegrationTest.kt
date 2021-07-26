package no.nav.familie.ba.sak.config

import no.nav.familie.ba.sak.common.DbContainerInitializer
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
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
        "mock-arbeidsfordeling",
        "mock-oauth",
        "integrasjonstest",
)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@AutoConfigureWireMock(port = 28085)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("integration")
abstract class AbstractSpringIntegrationTest
