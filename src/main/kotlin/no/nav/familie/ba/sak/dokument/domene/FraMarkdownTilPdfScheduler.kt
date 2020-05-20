package no.nav.familie.ba.sak.dokument.domene

import no.nav.familie.ba.sak.behandling.vedtak.VedtakRepository
import no.nav.familie.ba.sak.dokument.DokumentService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class FraMarkdownTilPdfScheduler(private val vedtakRepository: VedtakRepository,
                                 private val dokumentService: DokumentService) {

    @Scheduled(initialDelay = 1000, fixedDelay = Long.MAX_VALUE)
    fun konverterVedtaksbrevLagretSomMarkdownTilPdf() {
        LOGGER.info("konverterer gamle vedtaksbrev i markdown-format til PDF")
        vedtakRepository.findAll().filter { vedtak ->
            vedtak.stønadBrevMarkdown.isNotEmpty() && vedtak.stønadBrevPdF == null
        }.forEach {
            it.stønadBrevPdF = dokumentService.hentPdfForVedtak(vedtak = it)
            vedtakRepository.save(it)
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(FraMarkdownTilPdfScheduler::class.java)
    }
}