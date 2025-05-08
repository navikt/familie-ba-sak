package no.nav.familie.ba.sak.kjerne.vedtak.tilbakekrevingsvedtakmotregning

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingTilSimuleringService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class TilbakekrevingsvedtakMotregningService(
    private val tilbakekrevingsvedtakMotregningRepository: TilbakekrevingsvedtakMotregningRepository,
    private val loggService: LoggService,
    private val behandlingService: BehandlingHentOgPersisterService,
    private val tilbakestillBehandlingTilSimuleringService: TilbakestillBehandlingTilSimuleringService,
) {
    fun finnTilbakekrevingsvedtakMotregning(behandlingId: Long) = tilbakekrevingsvedtakMotregningRepository.finnTilbakekrevingsvedtakMotregningForBehandling(behandlingId)

    fun hentTilbakekrevingsvedtakMotregningEllerKastFunksjonellFeil(behandlingId: Long): TilbakekrevingsvedtakMotregning =
        finnTilbakekrevingsvedtakMotregning(behandlingId)
            ?: throw FunksjonellFeil("Tilbakekrevingsvedtak motregning finnes ikke for behandling $behandlingId. Oppdater fanen og prøv igjen.")

    @Transactional
    fun opprettTilbakekrevingsvedtakMotregning(behandlingId: Long) =
        finnTilbakekrevingsvedtakMotregning(behandlingId) ?: run {
            val behandling = behandlingService.hent(behandlingId)
            tilbakekrevingsvedtakMotregningRepository.save(
                TilbakekrevingsvedtakMotregning(
                    behandling = behandling,
                    samtykke = false,
                    heleBeløpetSkalKrevesTilbake = false,
                ),
            )
        }

    @Transactional
    fun oppdaterTilbakekrevingsvedtakMotregning(
        behandlingId: Long,
        samtykke: Boolean? = null,
        årsakTilFeilutbetaling: String? = null,
        vurderingAvSkyld: String? = null,
        varselDato: LocalDate? = null,
        heleBeløpetSkalKrevesTilbake: Boolean? = null,
    ): TilbakekrevingsvedtakMotregning {
        val tilbakekrevingsvedtakMotregning =
            hentTilbakekrevingsvedtakMotregningEllerKastFunksjonellFeil(behandlingId).apply {
                samtykke?.let {
                    this.samtykke = it
                    if (it) {
                        loggService.loggUlovfestetMotregningBenyttet(behandlingId)
                    }
                }
                årsakTilFeilutbetaling?.let {
                    this.årsakTilFeilutbetaling = it
                }
                vurderingAvSkyld?.let {
                    this.vurderingAvSkyld = it
                }
                varselDato?.let {
                    this.varselDato = it
                }
                heleBeløpetSkalKrevesTilbake?.let {
                    this.heleBeløpetSkalKrevesTilbake = it
                    tilbakestillBehandlingTilSimuleringService.tilbakestillBehandlingTilSimuering(behandlingId)
                }
            }

        return tilbakekrevingsvedtakMotregningRepository.save(tilbakekrevingsvedtakMotregning)
    }

    @Transactional
    fun slettTilbakekrevingsvedtakMotregning(behandlingId: Long) =
        finnTilbakekrevingsvedtakMotregning(behandlingId)?.let {
            tilbakekrevingsvedtakMotregningRepository.delete(it)
            if (it.samtykke) {
                loggService.loggUlovfestetMotregningAngret(behandlingId)
            }
        }
}
