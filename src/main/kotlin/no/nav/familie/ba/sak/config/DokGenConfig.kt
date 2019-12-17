package no.nav.familie.ba.sak.config

import no.nav.familie.ba.sak.behandling.DokGenKlient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
class DokGenConfig {

    @Bean
    fun dokgenKlient(@Value("\${FAMILIE_BA_DOKGEN_API_URL}") dokgenServiceUri: String,
                     @Autowired restTemplate: RestTemplate): DokGenKlient {
        return DokGenKlient(dokgenServiceUri, restTemplate)
    }
}