package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate

fun validerSatsendring(
    fom: LocalDate?,
    harBarnMedSeksårsdagPåFom: Boolean,
) {
    val satsendring = SatsService.finnSatsendring(fom ?: TIDENES_MORGEN)

    if (satsendring.isEmpty() && !harBarnMedSeksårsdagPåFom) {
        throw FunksjonellFeil(
            melding = "Begrunnelsen stemmer ikke med satsendring.",
            frontendFeilmelding = "Begrunnelsen stemmer ikke med satsendring. Vennligst velg en annen begrunnelse.",
        )
    }
}

fun validerVedtaksperiodeMedBegrunnelser(vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser) {
    if (vedtaksperiodeMedBegrunnelser.harFriteksterUtenStandardbegrunnelser()) {
        val fritekstUtenStandardbegrunnelserFeilmelding =
            "Fritekst kan kun brukes i kombinasjon med en eller flere begrunnelser. " + "Legg først til en ny begrunnelse eller fjern friteksten(e)."
        throw FunksjonellFeil(
            melding = fritekstUtenStandardbegrunnelserFeilmelding,
            frontendFeilmelding = fritekstUtenStandardbegrunnelserFeilmelding,
        )
    }
}

// Håpet er at denne skal kaste feil på sikt, men enn så lenge blir det for strengt. Logger for å se behovet.
fun List<UtvidetVedtaksperiodeMedBegrunnelser>.validerPerioderInneholderBegrunnelser(
    behandlingId: Long,
    fagsakId: Long,
) {
    this.forEach {
        it.validerMinstEnBegrunnelseValgt(behandlingId = behandlingId, fagsakId = fagsakId)
        it.validerMinstEnReduksjonsbegrunnelseVedReduksjon(behandlingId = behandlingId, fagsakId = fagsakId)
        it.validerMinstEnInnvilgetbegrunnelseVedInnvilgelse(behandlingId = behandlingId, fagsakId = fagsakId)
        it.validerMinstEnEndretUtbetalingbegrunnelseVedEndretUtbetaling(
            behandlingId = behandlingId,
            fagsakId = fagsakId,
        )
    }
}

private fun UtvidetVedtaksperiodeMedBegrunnelser.validerMinstEnEndretUtbetalingbegrunnelseVedEndretUtbetaling(
    behandlingId: Long,
    fagsakId: Long,
) {
    val erMuligÅVelgeEndretUtbetalingBegrunnelse =
        this.gyldigeBegrunnelser.any { it.vedtakBegrunnelseType.erEndretUtbetaling() }
    val erValgtEndretUtbetalingBegrunnelse =
        this.begrunnelser.any { it.standardbegrunnelse.vedtakBegrunnelseType.erEndretUtbetaling() }

    if (erMuligÅVelgeEndretUtbetalingBegrunnelse && !erValgtEndretUtbetalingBegrunnelse) {
        logger.info("Vedtaksperioden ${this.fom?.tilKortString() ?: ""} - ${this.tom?.tilKortString() ?: ""} mangler endretubetalingsbegrunnelse. Fagsak: $fagsakId, behandling: $behandlingId")
    }
}

private fun UtvidetVedtaksperiodeMedBegrunnelser.validerMinstEnInnvilgetbegrunnelseVedInnvilgelse(
    behandlingId: Long,
    fagsakId: Long,
) {
    val erMuligÅVelgeInnvilgetBegrunnelse =
        this.gyldigeBegrunnelser.any { it.vedtakBegrunnelseType.erInnvilget() }
    val erValgtInnvilgetBegrunnelse =
        this.begrunnelser.any { it.standardbegrunnelse.vedtakBegrunnelseType.erInnvilget() }

    if (erMuligÅVelgeInnvilgetBegrunnelse && !erValgtInnvilgetBegrunnelse) {
        logger.info("Vedtaksperioden ${this.fom?.tilKortString() ?: ""} - ${this.tom?.tilKortString() ?: ""} mangler innvilgelsebegrunnelse. Fagsak: $fagsakId, behandling: $behandlingId")
    }
}

private fun UtvidetVedtaksperiodeMedBegrunnelser.validerMinstEnReduksjonsbegrunnelseVedReduksjon(
    behandlingId: Long,
    fagsakId: Long,
) {
    val erMuligÅVelgeReduksjonBegrunnelse =
        this.gyldigeBegrunnelser.any { it.vedtakBegrunnelseType.erReduksjon() }
    val erValgtReduksjonBegrunnelse =
        this.begrunnelser.any { it.standardbegrunnelse.vedtakBegrunnelseType.erReduksjon() }

    if (erMuligÅVelgeReduksjonBegrunnelse && !erValgtReduksjonBegrunnelse) {
        logger.info("Vedtaksperioden ${this.fom?.tilKortString() ?: ""} - ${this.tom?.tilKortString() ?: ""} mangler reduksjonsbegrunnelse. Fagsak: $fagsakId, behandling: $behandlingId")
    }
}

private fun UtvidetVedtaksperiodeMedBegrunnelser.validerMinstEnBegrunnelseValgt(
    behandlingId: Long,
    fagsakId: Long,
) {
    if (this.begrunnelser.isEmpty()) {
        logger.info("Vedtaksperioden ${this.fom?.tilKortString() ?: ""} - ${this.tom?.tilKortString() ?: ""} har ingen begrunnelser knyttet til seg. Fagsak: $fagsakId, behandling: $behandlingId")
    }
}

val logger: Logger = LoggerFactory.getLogger("validerPerioderInneholderBegrunnelserLogger")
