package no.nav.familie.ba.sak.kjerne.vedtak.forenklettilbakekrevingvedtak

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ForenkletTilbakekrevingVedtakService(
    private val forenkletTilbakekrevingVedtakRepository: ForenkletTilbakekrevingVedtakRepository,
    private val loggService: LoggService,
) {
    @Transactional(readOnly = true)
    fun finnForenkletTilbakekrevingVedtak(behandlingId: Long) = forenkletTilbakekrevingVedtakRepository.finnForenkletTilbakekrevingVedtakForBehandling(behandlingId)

    @Transactional
    fun opprettForenkletTilbakekrevingVedtak(behandlingId: Long) =
        finnForenkletTilbakekrevingVedtak(behandlingId) ?: run {
            loggService.loggForenkletTilbakekrevingVedtakOpprettet(behandlingId)
            forenkletTilbakekrevingVedtakRepository.save(
                ForenkletTilbakekrevingVedtak(
                    behandlingId = behandlingId,
                    samtykke = false,
                    fritekst = STANDARD_TEKST_FORENKLET_TILBAKEKREVING_VEDTAK,
                ),
            )
        }

    @Transactional
    fun oppdaterSamtykkePåForenkletTilbakekrevingVedtak(
        behandlingId: Long,
        samtykke: Boolean,
    ): ForenkletTilbakekrevingVedtak {
        val forenkletTilbakekrevingVedtak =
            hentForenkletTilbakekrevingVedtakEllerKastFunksjonellFeil(behandlingId).apply {
                this.samtykke = samtykke
                loggService.loggForenkletTilbakekrevingVedtakOppdatertSamtykke(behandlingId)
            }

        return forenkletTilbakekrevingVedtakRepository.save(forenkletTilbakekrevingVedtak)
    }

    @Transactional
    fun oppdaterFritekstPåForenkletTilbakekrevingVedtak(
        behandlingId: Long,
        fritekst: String,
    ): ForenkletTilbakekrevingVedtak {
        val forenkletTilbakekrevingVedtak =
            hentForenkletTilbakekrevingVedtakEllerKastFunksjonellFeil(behandlingId).apply {
                this.fritekst = fritekst
                loggService.loggForenkletTilbakekrevingVedtakOppdatertFritekst(behandlingId)
            }

        forenkletTilbakekrevingVedtakRepository.save(forenkletTilbakekrevingVedtak)

        return forenkletTilbakekrevingVedtak
    }

    @Transactional
    fun slettForenkletTilbakekrevingVedtak(behandlingId: Long) =
        finnForenkletTilbakekrevingVedtak(behandlingId)?.let {
            forenkletTilbakekrevingVedtakRepository.delete(it)
            loggService.loggForenkletTilbakekrevingVedtakSlettet(behandlingId)
        }

    private fun hentForenkletTilbakekrevingVedtakEllerKastFunksjonellFeil(behandlingId: Long): ForenkletTilbakekrevingVedtak =
        finnForenkletTilbakekrevingVedtak(behandlingId)
            ?: throw FunksjonellFeil("Forenklet tilbakekreving vedtak finnes ikke for behandling $behandlingId. Oppdater fanen og prøv igjen.")

    companion object {
        //TODO: Denne endres på senere når det er klart hva standard tekst skal være
        private const val STANDARD_TEKST_FORENKLET_TILBAKEKREVING_VEDTAK = "TEKST"
    }
}
