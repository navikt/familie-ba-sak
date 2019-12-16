package no.nav.familie.ba.sak.dokgen

import no.nav.familie.ba.sak.behandling.DokGenKlient
import org.mockito.Mockito.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
class DokgenTestConfig {

    @Bean
    @Profile("mock-dokgen")
    @Primary
    fun mockDokGenKlient(): DokGenKlient {
        val dokgenKlient = mock(DokGenKlient::class.java)
        `when`(dokgenKlient.hentMarkdownForMal(anyString(), anyString())).thenReturn("# Markdown")

        return dokgenKlient
    }
}