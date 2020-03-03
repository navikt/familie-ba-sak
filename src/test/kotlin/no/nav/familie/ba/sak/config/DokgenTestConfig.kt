package no.nav.familie.ba.sak.config

import no.nav.familie.ba.sak.dokument.DokGenKlient
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
    fun mockDokGenService(): DokGenKlient {
        //eliminate complain from Mockito of null parameter
        fun <T> any(): T = Mockito.any<T>()

        val dokgenService = mock(DokGenKlient::class.java)
        `when`(dokgenService.lagHtmlFraMarkdown("Innvilget", "TEST_MARKDOWN_MOCKUP")).thenReturn("<HTML>HTML_MOCKUP</HTML>")
        `when`(dokgenService.lagHtmlFraMarkdown("Avslag", "TEST_MARKDOWN_MOCKUP")).thenReturn("<HTML>HTML_MOCKUP</HTML>")
        `when`(dokgenService.lagHtmlFraMarkdown("Opphørt", "TEST_MARKDOWN_MOCKUP")).thenReturn("<HTML>HTML_MOCKUP</HTML>")
        `when`(dokgenService.hentStønadBrevMarkdown(any(), any())).thenReturn("TEST_MARKDOWN_MOCKUP")
        return dokgenService
    }

    @Bean
    @Profile("mock-dokgen-negative")
    @Primary
    fun mockDokGenNegativeService(): DokGenKlient {
        //eliminate complain from Mockito of null parameter
        fun <T> any(): T = Mockito.any<T>()

        val dokgenService = mock(DokGenKlient::class.java)
        `when`(dokgenService.lagHtmlFraMarkdown("Innvilget", "TEST_MARKDOWN_MOCKUP")).thenThrow(IllegalStateException())
        `when`(dokgenService.hentStønadBrevMarkdown(any(), any())).thenReturn("TEST_MARKDOWN_MOCKUP")
        return dokgenService
    }
}