package no.nav.familie.ba.sak.dokument

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.kontrakter.felles.Ressurs
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ActiveProfiles("dev")
@Tag("integration")
class DokumentControllerTest(
        @Autowired
        private val dokumentService: DokumentService
) {

    @Test
    @Tag("integration")
    fun `Test hent html vedtak`() {
        val mockDokumentService: DokumentService = mockk()
        val mockDokumentController = DokumentController(mockDokumentService)
        every { mockDokumentService.hentHtmlForVedtak(any()) } returns Ressurs.success("mock_html")

        val response = mockDokumentController.hentHtmlVedtak(1)
        assert(response.status == Ressurs.Status.SUKSESS)
    }

    @Test
    @Tag("integration")
    fun `Hent HTML vedtaksbrev Negative'`() {
        val failRess = dokumentService.hentHtmlForVedtak(100)
        Assertions.assertEquals(Ressurs.Status.FEILET, failRess.status)
    }
}