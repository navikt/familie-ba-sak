package no.nav.familie.ba.sak.kjerne.vedtak.forenklettilbakekrevingsvedtak

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ForenkletTilbakekrevingsvedtakService(
    private val forenkletTilbakekrevingsvedtakRepository: ForenkletTilbakekrevingsvedtakRepository,
    private val loggService: LoggService,
) {
    fun finnForenkletTilbakekrevingsvedtak(behandlingId: Long) = forenkletTilbakekrevingsvedtakRepository.finnForenkletTilbakekrevingsvedtakForBehandling(behandlingId)

    @Transactional
    fun opprettForenkletTilbakekrevingsvedtak(behandlingId: Long) =
        finnForenkletTilbakekrevingsvedtak(behandlingId) ?: run {
            loggService.loggForenkletTilbakekrevingsvedtakOpprettet(behandlingId)
            forenkletTilbakekrevingsvedtakRepository.save(
                ForenkletTilbakekrevingsvedtak(
                    behandlingId = behandlingId,
                    samtykke = false,
                    fritekst = STANDARD_TEKST_FORENKLET_TILBAKEKREVINGSVEDTAK,
                ),
            )
        }

    @Transactional
    fun oppdaterSamtykkePåForenkletTilbakekrevingsvedtak(
        behandlingId: Long,
        samtykke: Boolean,
    ): ForenkletTilbakekrevingsvedtak {
        val forenkletTilbakekrevingsvedtak =
            hentForenkletTilbakekrevingsvedtakEllerKastFunksjonellFeil(behandlingId).apply {
                this.samtykke = samtykke
                loggService.loggForenkletTilbakekrevingsvedtakOppdatertSamtykke(behandlingId)
            }

        return forenkletTilbakekrevingsvedtakRepository.save(forenkletTilbakekrevingsvedtak)
    }

    @Transactional
    fun oppdaterFritekstPåForenkletTilbakekrevingsvedtak(
        behandlingId: Long,
        fritekst: String,
    ): ForenkletTilbakekrevingsvedtak {
        val forenkletTilbakekrevingsvedtak =
            hentForenkletTilbakekrevingsvedtakEllerKastFunksjonellFeil(behandlingId).apply {
                this.fritekst = fritekst
                loggService.loggForenkletTilbakekrevingsvedtakOppdatertFritekst(behandlingId)
            }

        forenkletTilbakekrevingsvedtakRepository.save(forenkletTilbakekrevingsvedtak)

        return forenkletTilbakekrevingsvedtak
    }

    @Transactional
    fun slettForenkletTilbakekrevingsvedtak(behandlingId: Long) =
        finnForenkletTilbakekrevingsvedtak(behandlingId)?.let {
            forenkletTilbakekrevingsvedtakRepository.delete(it)
            loggService.loggForenkletTilbakekrevingsvedtakSlettet(behandlingId)
        }

    private fun hentForenkletTilbakekrevingsvedtakEllerKastFunksjonellFeil(behandlingId: Long): ForenkletTilbakekrevingsvedtak =
        finnForenkletTilbakekrevingsvedtak(behandlingId)
            ?: throw FunksjonellFeil("Forenklet tilbakekrevingsvedtak finnes ikke for behandling $behandlingId. Oppdater fanen og prøv igjen.")

    companion object {
        // TODO: Denne endres på senere når det er klart hva standard tekst skal være
        private const val STANDARD_TEKST_FORENKLET_TILBAKEKREVINGSVEDTAK = "TEKST"
    }
}
