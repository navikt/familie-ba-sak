package no.nav.familie.ba.sak.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.dokument.DokGenKlient
import no.nav.familie.ba.sak.dokument.DokumentService
import no.nav.familie.ba.sak.dokument.RestDokument
import no.nav.familie.ba.sak.dokument.testDokumentHeaderFelter
import no.nav.familie.kontrakter.felles.Ressurs.Companion.success
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@TestConfiguration
class DokgenTestConfig {

    @Bean
    @Profile("mock-dokgen")
    @Primary
    fun mockDokumentService(): DokumentService {
        val dokumentService: DokumentService = mockk()
        every { dokumentService.hentBrevForVedtak(any()) } returns success(RestDokument("pdf".toByteArray(),"<HTML>HTML_MOCKUP</HTML>"))
        every { dokumentService.hentStønadBrevMarkdown(any(), any(), any()) } returns "Markdown mock"
        every { dokumentService.hentPdfForVedtak(any()) } returns TEST_PDF
        return dokumentService
    }

    @Bean
    @Profile("mock-dokgen-negative")
    @Primary
    fun mockDokGenNegativeService(): DokGenKlient {
        val dokgenService: DokGenKlient = mockk()
        every { dokgenService.lagHtmlFraMarkdown("Innvilget", "TEST_MARKDOWN_MOCKUP", testDokumentHeaderFelter) } throws IllegalStateException()
        return dokgenService
    }
}