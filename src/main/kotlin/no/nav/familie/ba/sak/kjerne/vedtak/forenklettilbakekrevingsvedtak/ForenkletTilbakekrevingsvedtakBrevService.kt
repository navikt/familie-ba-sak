package no.nav.familie.ba.sak.kjerne.vedtak.forenklettilbakekrevingsvedtak

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.kjerne.brev.DokumentGenereringService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ForenkletTilbakekrevingsvedtakBrevService(
    private val forenkletTilbakekrevingsvedtakRepository: ForenkletTilbakekrevingsvedtakRepository,
    private val dokumentGenereringService: DokumentGenereringService,
) {
    private fun hentForenkletTilbakekrevingsvedtakEllerKastFunksjonellFeil(behandlingId: Long): ForenkletTilbakekrevingsvedtak =
        forenkletTilbakekrevingsvedtakRepository.finnForenkletTilbakekrevingsvedtakForBehandling(behandlingId)
            ?: throw FunksjonellFeil("Forenklet tilbakekrevingsvedtak finnes ikke for behandling $behandlingId. Oppdater fanen og prøv igjen.")

    @Transactional
    fun opprettOgLagreForenkletTilbakekrevingsvedtakPdf(
        behandlingId: Long,
    ): ForenkletTilbakekrevingsvedtak {
        val forenkletTilbakekrevingsvedtak = hentForenkletTilbakekrevingsvedtakEllerKastFunksjonellFeil(behandlingId)
        val pdf = dokumentGenereringService.genererBrevForForenkletTilbakekrevingsvedtak(forenkletTilbakekrevingsvedtak)

        forenkletTilbakekrevingsvedtak.vedtakPdf = pdf
        forenkletTilbakekrevingsvedtakRepository.saveAndFlush(forenkletTilbakekrevingsvedtak)

        return forenkletTilbakekrevingsvedtak
    }
}
