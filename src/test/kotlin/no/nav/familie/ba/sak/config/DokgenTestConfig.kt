package no.nav.familie.ba.sak.config

import no.nav.familie.ba.sak.behandling.DokGenService
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@TestConfiguration
class DokgenTestConfig {

    @Bean
    @Profile("mock-dokgen")
    @Primary
    fun mockDokGenService(): DokGenService {
        //eliminate complain from Mockito of null parameter
        fun <T> any(): T = Mockito.any<T>()

        val dokgenService = mock(DokGenService::class.java)
        `when`(dokgenService.lagHtmlFraMarkdown("Innvilget", "TEST_MARKDOWN_MOCKUP")).thenReturn("<HTML>HTML_MOCKUP</HTML>")
        `when`(dokgenService.hentStønadBrevMarkdown(any())).thenReturn("TEST_MARKDOWN_MOCKUP")
        return dokgenService
    }

    @Bean
    @Profile("mock-dokgen-negative")
    @Primary
    fun mockDokGenNegativeService(): DokGenService {
        //eliminate complain from Mockito of null parameter
        fun <T> any(): T = Mockito.any<T>()

        val dokgenService = mock(DokGenService::class.java)
        `when`(dokgenService.lagHtmlFraMarkdown("Innvilget", "TEST_MARKDOWN_MOCKUP")).thenThrow(RuntimeException())
        `when`(dokgenService.hentStønadBrevMarkdown(any())).thenReturn("TEST_MARKDOWN_MOCKUP")
        return dokgenService
    }
}