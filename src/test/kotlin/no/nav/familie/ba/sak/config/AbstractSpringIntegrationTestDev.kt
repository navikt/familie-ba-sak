package no.nav.familie.ba.sak.config

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(classes = [ApplicationConfig::class])
@ActiveProfiles(
    "dev",
    "mock-rest-template-config",
    "mock-oauth",
    "mock-pdl",
    "mock-infotrygd-barnetrygd",
    "mock-infotrygd-feed",
    "mock-økonomi",
    "mock-brev-klient"
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureWireMock(port = 0)
@Tag("integration")
@DirtiesContext
abstract class AbstractSpringIntegrationTestDev
