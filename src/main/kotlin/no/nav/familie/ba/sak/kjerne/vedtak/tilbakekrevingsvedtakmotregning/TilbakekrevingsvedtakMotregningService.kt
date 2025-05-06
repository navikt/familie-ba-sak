package no.nav.familie.ba.sak.kjerne.vedtak.tilbakekrevingsvedtakmotregning

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

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
                }
            }

        loggService.loggTilbakekrevingsvedtakMotregningOppdatert(behandlingId)
        return tilbakekrevingsvedtakMotregningRepository.save(tilbakekrevingsvedtakMotregning)
    }

    @Transactional
    fun slettTilbakekrevingsvedtakMotregning(behandlingId: Long) =
        finnTilbakekrevingsvedtakMotregning(behandlingId)?.let {
            tilbakekrevingsvedtakMotregningRepository.delete(it)
            loggService.loggTilbakekrevingsvedtakMotregningSlettet(behandlingId)
        }
}
