package no.nav.familie.ba.sak.kjerne.vedtak.tilbakekrevingsvedtakmotregning

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TilbakekrevingsvedtakMotregningService(
    private val tilbakekrevingsvedtakMotregningRepository: TilbakekrevingsvedtakMotregningRepository,
    private val loggService: LoggService,
    private val behandlingService: BehandlingHentOgPersisterService,
) {
    fun finnTilbakekrevingsvedtakMotregning(behandlingId: Long) = tilbakekrevingsvedtakMotregningRepository.finnTilbakekrevingsvedtakMotregningForBehandling(behandlingId)

    fun hentTilbakekrevingsvedtakMotregningEllerKastFunksjonellFeil(behandlingId: Long): TilbakekrevingsvedtakMotregning =
        finnTilbakekrevingsvedtakMotregning(behandlingId)
            ?: throw FunksjonellFeil("Tilbakekrevingsvedtak motregning finnes ikke for behandling $behandlingId. Oppdater fanen og prøv igjen.")

    @Transactional
    fun opprettTilbakekrevingsvedtakMotregning(behandlingId: Long) =
        finnTilbakekrevingsvedtakMotregning(behandlingId) ?: run {
            loggService.loggTilbakekrevingsvedtakMotregningOpprettet(behandlingId)
            val behandling = behandlingService.hent(behandlingId)
            tilbakekrevingsvedtakMotregningRepository.save(
                TilbakekrevingsvedtakMotregning(
                    behandling = behandling,
                    samtykke = false,
                    fritekst = STANDARD_TEKST_TILBAKEKREVINGSVEDTAK_MOTREGNING,
                ),
            )
        }

    @Transactional
    fun oppdaterSamtykkePåTilbakekrevingsvedtakMotregning(
        behandlingId: Long,
        samtykke: Boolean,
    ): TilbakekrevingsvedtakMotregning {
        val tilbakekrevingsvedtakMotregning =
            hentTilbakekrevingsvedtakMotregningEllerKastFunksjonellFeil(behandlingId).apply {
                this.samtykke = samtykke
                loggService.loggTilbakekrevingsvedtakMotregningOppdatertSamtykke(behandlingId)
            }

        return tilbakekrevingsvedtakMotregningRepository.save(tilbakekrevingsvedtakMotregning)
    }

    @Transactional
    fun oppdaterFritekstPåTilbakekrevingsvedtakMotregning(
        behandlingId: Long,
        fritekst: String,
    ): TilbakekrevingsvedtakMotregning {
        val tilbakekrevingsvedtakMotregning =
            hentTilbakekrevingsvedtakMotregningEllerKastFunksjonellFeil(behandlingId).apply {
                this.fritekst = fritekst
                loggService.loggTilbakekrevingsvedtakMotregningOppdatertFritekst(behandlingId)
            }

        tilbakekrevingsvedtakMotregningRepository.save(tilbakekrevingsvedtakMotregning)

        return tilbakekrevingsvedtakMotregning
    }

    @Transactional
    fun slettTilbakekrevingsvedtakMotregning(behandlingId: Long) =
        finnTilbakekrevingsvedtakMotregning(behandlingId)?.let {
            tilbakekrevingsvedtakMotregningRepository.delete(it)
            loggService.loggTilbakekrevingsvedtakMotregningSlettet(behandlingId)
        }

    companion object {
        // TODO: Denne endres på senere når det er klart hva standard tekst skal være
        private const val STANDARD_TEKST_TILBAKEKREVINGSVEDTAK_MOTREGNING = "TEKST"
    }
}
