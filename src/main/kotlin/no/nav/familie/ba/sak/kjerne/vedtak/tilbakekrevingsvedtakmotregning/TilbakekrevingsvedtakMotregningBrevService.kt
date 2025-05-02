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

        validerAtFritekstfelterErUtfylt(tilbakekrevingsvedtakMotregning)

        val pdf = dokumentGenereringService.genererBrevForTilbakekrevingsvedtakMotregning(tilbakekrevingsvedtakMotregning)

        tilbakekrevingsvedtakMotregning.vedtakPdf = pdf
        tilbakekrevingsvedtakMotregningRepository.saveAndFlush(tilbakekrevingsvedtakMotregning)

        return tilbakekrevingsvedtakMotregning
    }

    private fun validerAtFritekstfelterErUtfylt(tilbakekrevingsvedtakMotregning: TilbakekrevingsvedtakMotregning) {
        if (tilbakekrevingsvedtakMotregning.årsakTilFeilutbetaling == null || tilbakekrevingsvedtakMotregning.vurderingAvSkyld == null) {
            throw FunksjonellFeil(
                "Fritekstfeltene for årsak til feilutbetaling og vurdering av skyld må være utfylt for å generere brevet.",
            )
        }
    }
}
