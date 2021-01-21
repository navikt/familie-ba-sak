package no.nav.familie.ba.sak.dokument

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.brev.BrevService
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.kontrakter.felles.Ressurs
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
        private val dokumentService: DokumentService,
        @Autowired
        private val behandlingService: BehandlingService,
        @Autowired
        private val brevService: BrevService,
        @Autowired
        private val featureToggleService: FeatureToggleService

) {

    @Test
    @Tag("integration")
    fun `Test generer vedtaksbrev`() {
        val mockDokumentService: DokumentService = mockk()
        val vedtakService: VedtakService = mockk(relaxed = true)
        val fagsakService: FagsakService = mockk()
        val mockDokumentController = DokumentController(mockDokumentService, vedtakService, behandlingService, fagsakService, brevService, featureToggleService)
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
        val fagsakService: FagsakService = mockk()
        val mockDokumentController = DokumentController(mockDokumentService, vedtakService, behandlingService, fagsakService, brevService, featureToggleService)
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