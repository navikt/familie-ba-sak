package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class VedtakPeriodeValidering(
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val vedtakService: VedtakService,
) {
    fun validerPerioderInneholderBegrunnelser(behandlingId: Long) {
        val vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId = behandlingId)
        val utvidetVedtaksperioder = vedtaksperiodeService.hentUtvidetVedtaksperiodeMedBegrunnelser(vedtak)
        utvidetVedtaksperioder.forEach {
            it.validerMinstEnBegrunnelseValgt()
            it.validerMinstEnReduksjonsbegrunnelseVedReduksjon()
            it.validerMinstEnInnvilgetbegrunnelseVedInnvilgelse()
            it.validerMinstEnEndretUtbetalingbegrunnelseVedEndretUtbetaling()
        }
    }

    private fun UtvidetVedtaksperiodeMedBegrunnelser.validerMinstEnEndretUtbetalingbegrunnelseVedEndretUtbetaling() {
        val erMuligÅVelgeEndretUtbetalingBegrunnelse =
            this.gyldigeBegrunnelser.any { it.vedtakBegrunnelseType == VedtakBegrunnelseType.ENDRET_UTBETALING }
        val erValgtEndretUtbetalingBegrunnelse =
            this.begrunnelser.any { it.vedtakBegrunnelseType == VedtakBegrunnelseType.ENDRET_UTBETALING }

        if (erMuligÅVelgeEndretUtbetalingBegrunnelse && !erValgtEndretUtbetalingBegrunnelse) {
            throw FunksjonellFeil(
                melding = "Vedtaket har ikke endretubetalingsbegrunnelse for periode med endret utbetaling",
                frontendFeilmelding = "Vedtaksperioden ${this.fom?.tilKortString() ?: ""} - ${this.tom?.tilKortString() ?: ""} mangler endretubetalingsbegrunnelse"
            )
        }
    }

    private fun UtvidetVedtaksperiodeMedBegrunnelser.validerMinstEnInnvilgetbegrunnelseVedInnvilgelse() {
        val erMuligÅVelgeInnvilgetBegrunnelse =
            this.gyldigeBegrunnelser.any { it.vedtakBegrunnelseType == VedtakBegrunnelseType.INNVILGET }
        val erValgtInnvilgetBegrunnelse =
            this.begrunnelser.any { it.vedtakBegrunnelseType == VedtakBegrunnelseType.INNVILGET }

        if (erMuligÅVelgeInnvilgetBegrunnelse && !erValgtInnvilgetBegrunnelse) {
            // Er ikke helt sikker på om dette stemmer for alle perioder så logger dette enn så lenge
            logger.warn("Vedtaksperioden ${this.fom?.tilKortString() ?: ""} - ${this.tom?.tilKortString() ?: ""} mangler innvilgelsebegrunnelse")
            /*throw FunksjonellFeil(
                    melding = "Vedtaket har ikke innvilgelsebegrunnelse for periode med økning i beløp",
                    frontendFeilmelding = "Vedtaksperioden ${periode.fom ?: ""} - ${periode.tom ?: ""} mangler innvilgelsebegrunnelse"
                )*/
        }
    }

    private fun UtvidetVedtaksperiodeMedBegrunnelser.validerMinstEnReduksjonsbegrunnelseVedReduksjon() {
        val erMuligÅVelgeReduksjonBegrunnelse =
            this.gyldigeBegrunnelser.any { it.vedtakBegrunnelseType == VedtakBegrunnelseType.REDUKSJON }
        val erValgtReduksjonBegrunnelse =
            this.begrunnelser.any { it.vedtakBegrunnelseType == VedtakBegrunnelseType.REDUKSJON }

        if (erMuligÅVelgeReduksjonBegrunnelse && !erValgtReduksjonBegrunnelse) {
            // Er ikke helt sikker på om dette stemmer for alle perioder så logger dette enn så lenge
            logger.warn("Vedtaksperioden ${this.fom?.tilKortString() ?: ""} - ${this.tom?.tilKortString() ?: ""} mangler reduksjonsbegrunnelse")
            /*throw FunksjonellFeil(
                    melding = "Vedtaket har ikke reduksjonsbegrunnelse for periode med reduksjon",
                    frontendFeilmelding = "Vedtaksperioden ${periode.fom ?: ""} - ${periode.tom ?: ""} mangler reduksjonsbegrunnelse"
                )*/
        }
    }

    private fun UtvidetVedtaksperiodeMedBegrunnelser.validerMinstEnBegrunnelseValgt() {
        if (this.begrunnelser.isEmpty()) {
            throw FunksjonellFeil(
                "Vedtaksperioden ${this.fom?.tilKortString() ?: ""} - ${this.tom?.tilKortString() ?: ""} har ingen begrunnelser knyttet til seg.",
            )
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}
