package no.nav.familie.ba.sak.kjerne.vedtak.tilbakekrevingsvedtakmotregning

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.kjerne.brev.DokumentGenereringService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TilbakekrevingsvedtakMotregningBrevService(
    private val tilbakekrevingsvedtakMotregningRepository: TilbakekrevingsvedtakMotregningRepository,
    private val dokumentGenereringService: DokumentGenereringService,
) {
    private fun hentTilbakekrevingsvedtakMotregningEllerKastFunksjonellFeil(behandlingId: Long): TilbakekrevingsvedtakMotregning =
        tilbakekrevingsvedtakMotregningRepository.finnTilbakekrevingsvedtakMotregningForBehandling(behandlingId)
            ?: throw FunksjonellFeil("Tilbakekrevingsvedtak motregning finnes ikke for behandling $behandlingId. Oppdater fanen og prøv igjen.")

    @Transactional
    fun opprettOgLagreTilbakekrevingsvedtakMotregningPdf(
        behandlingId: Long,
    ): TilbakekrevingsvedtakMotregning {
        val tilbakekrevingsvedtakMotregning = hentTilbakekrevingsvedtakMotregningEllerKastFunksjonellFeil(behandlingId)
        val pdf = dokumentGenereringService.genererBrevForTilbakekrevingsvedtakMotregning(tilbakekrevingsvedtakMotregning)

        tilbakekrevingsvedtakMotregning.vedtakPdf = pdf
        tilbakekrevingsvedtakMotregningRepository.saveAndFlush(tilbakekrevingsvedtakMotregning)

        return tilbakekrevingsvedtakMotregning
    }
}
