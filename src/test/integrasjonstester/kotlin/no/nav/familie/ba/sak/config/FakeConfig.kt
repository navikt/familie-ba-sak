package no.nav.familie.ba.sak.config

import no.nav.familie.ba.sak.fake.FakeIntegrasjonClient
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.web.client.RestOperations

@TestConfiguration
@Profile("integrasjonstest")
class FakeConfig {
    @Bean
    @Primary
    fun fakeIntegrasjonClient(restOperations: RestOperations): FakeIntegrasjonClient = FakeIntegrasjonClient(restOperations)
}
