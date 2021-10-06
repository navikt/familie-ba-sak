package no.nav.familie.ba.sak.kjerne.dokument

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTestDev
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class DokumentControllerTest(
    @Autowired
    private val dokumentService: DokumentService,
    @Autowired
    private val behandlingService: BehandlingService,
) : AbstractSpringIntegrationTestDev() {

    final val mockDokumentService: DokumentService = mockk()
    final val vedtakService: VedtakService = mockk(relaxed = true)
    final val fagsakService: FagsakService = mockk()
    final val tilgangService: TilgangService = mockk(relaxed = true)
    val mockDokumentController =
        DokumentController(mockDokumentService, vedtakService, behandlingService, fagsakService, tilgangService, mockk(relaxed = true), mockk(relaxed = true))

    @Test
    @Tag("integration")
    fun `Test generer vedtaksbrev`() {
        every { vedtakService.hent(any()) } returns lagVedtak()
        every { mockDokumentService.genererBrevForVedtak(any()) } returns "pdf".toByteArray()

        val response = mockDokumentController.genererVedtaksbrev(1)
        assert(response.status == Ressurs.Status.SUKSESS)
    }

    @Test
    @Tag("integration")
    fun `Test hent pdf vedtak`() {
        every { vedtakService.hent(any()) } returns lagVedtak()
        every { mockDokumentService.hentBrevForVedtak(any()) } returns Ressurs.success("pdf".toByteArray())

        val response = mockDokumentController.hentVedtaksbrev(1)
        assert(response.status == Ressurs.Status.SUKSESS)
    }

    @Test
    @Tag("integration")
    fun `Kast feil ved hent av vedtaksbrev når det ikke er generert brev`() {
        assertThrows<Feil> {
            dokumentService.hentBrevForVedtak(lagVedtak())
        }
    }
}
