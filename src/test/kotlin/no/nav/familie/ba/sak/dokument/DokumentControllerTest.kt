package no.nav.familie.ba.sak.dokument

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.kontrakter.felles.Ressurs
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.lang.IllegalStateException

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
    fun `Test generer vedtaksbrev`() {
        val mockDokumentService: DokumentService = mockk()
        val vedtakService: VedtakService = mockk(relaxed = true)
        val mockDokumentController = DokumentController(mockDokumentService, vedtakService)
        every { vedtakService.hent(any()) } returns lagVedtak()
        every { mockDokumentService.genererBrevForVedtak(any()) } returns "pdf".toByteArray()

        val response = mockDokumentController.genererVedtaksbrev(1)
        assert(response.status == Ressurs.Status.SUKSESS)
    }

    @Test
    @Tag("integration")
    fun `Test hent pdf vedtak`() {
        val mockDokumentService: DokumentService = mockk()
        val vedtakService: VedtakService = mockk()
        val mockDokumentController = DokumentController(mockDokumentService, vedtakService)
        every { vedtakService.hent(any()) } returns lagVedtak()
        every { mockDokumentService.hentBrevForVedtak(any()) } returns Ressurs.success("pdf".toByteArray())

        val response = mockDokumentController.hentVedtaksbrev(1)
        assert(response.status == Ressurs.Status.SUKSESS)
    }

    @Test
    @Tag("integration")
    fun `Hent pdf vedtaksbrev Negative'`() {
        assertThrows<IllegalStateException> {
            dokumentService.hentBrevForVedtak(lagVedtak())
        }
    }
}