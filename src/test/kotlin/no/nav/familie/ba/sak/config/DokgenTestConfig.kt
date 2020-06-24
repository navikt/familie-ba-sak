package no.nav.familie.ba.sak.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.dokument.DokGenKlient
import no.nav.familie.ba.sak.dokument.DokumentService
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
        every { dokumentService.hentBrevForVedtak(any()) } returns success("pdf".toByteArray())
        every { dokumentService.genererBrevForVedtak(any()) } returns TEST_PDF
        return dokumentService
    }

    @Bean
    @Profile("mock-dokgen-klient")
    @Primary
    fun mockDokGenKlient(): DokGenKlient {
        val dokGenKlient: DokGenKlient = mockk()
        every { dokGenKlient.lagPdfForMal(any(), any()) } returns TEST_PDF
        return dokGenKlient
    }

}