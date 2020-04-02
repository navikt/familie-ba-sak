package no.nav.familie.ba.sak.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.dokument.DokGenKlient
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
        val dokgenService: DokGenKlient = mockk()
        every { dokgenService.lagHtmlFraMarkdown("Innvilget", "TEST_MARKDOWN_MOCKUP") } returns "<HTML>HTML_MOCKUP</HTML>"
        every { dokgenService.lagHtmlFraMarkdown("Avslag", "TEST_MARKDOWN_MOCKUP") } returns "<HTML>HTML_MOCKUP</HTML>"
        every { dokgenService.lagHtmlFraMarkdown("Opphørt", "TEST_MARKDOWN_MOCKUP") } returns "<HTML>HTML_MOCKUP</HTML>"
        every { dokgenService.hentStønadBrevMarkdown(any(), any(), any()) } returns "TEST_MARKDOWN_MOCKUP"
        return dokgenService
    }

    @Bean
    @Profile("mock-dokgen-negative")
    @Primary
    fun mockDokGenNegativeService(): DokGenKlient {
        val dokgenService: DokGenKlient = mockk()
        every { dokgenService.lagHtmlFraMarkdown("Innvilget", "TEST_MARKDOWN_MOCKUP") } throws IllegalStateException()
        every { dokgenService.hentStønadBrevMarkdown(any(), any(), any()) } returns "TEST_MARKDOWN_MOCKUP"
        return dokgenService
    }
}