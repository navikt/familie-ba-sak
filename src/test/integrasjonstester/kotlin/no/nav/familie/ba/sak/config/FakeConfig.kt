package no.nav.familie.ba.sak.config

import no.nav.familie.ba.sak.fake.FakeIntegrasjonClient
import no.nav.familie.ba.sak.fake.FakeValutakursRestClient
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.web.client.RestOperations

@TestConfiguration
class FakeConfig {
    @Bean
    @Primary
    @Profile("fake-integrasjon-client")
    fun fakeIntegrasjonClient(restOperations: RestOperations): FakeIntegrasjonClient = FakeIntegrasjonClient(restOperations)

    @Bean
    @Primary
    @Profile("fake-valutakurs-rest-client")
    fun fakeValutakursRestClient(restOperations: RestOperations): FakeValutakursRestClient = FakeValutakursRestClient(restOperations)
}
