package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.NullablePeriode
import no.nav.familie.ba.sak.kjerne.brev.hentPersonidenterGjeldendeForBegrunnelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.TriggesAv
import no.nav.familie.ba.sak.kjerne.vedtak.domene.hentRelevanteEndringsperioderForBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.RestVedtaksbegrunnelse

data class BrevBegrunnelseGrunnlag(
    val standardbegrunnelse: Standardbegrunnelse,
    val triggesAv: TriggesAv,
) {
    fun tilBrevBegrunnelseGrunnlagMedPersoner(
        periode: NullablePeriode,
        vedtaksperiodetype: Vedtaksperiodetype,
        restBehandlingsgrunnlagForBrev: RestBehandlingsgrunnlagForBrev,
        identerMedUtbetalingPåPeriode: List<String>,
        erFørsteVedtaksperiodePåFagsak: Boolean,
        erUregistrerteBarnPåbehandling: Boolean,
        barnPersonIdentMedReduksjon: List<String>,
        erIngenOverlappVedtaksperiodeTogglePå: Boolean,
        minimerteUtbetalingsperiodeDetaljer: List<MinimertUtbetalingsperiodeDetalj>,
    ): List<BrevBegrunnelseGrunnlagMedPersoner> {

        if (this.standardbegrunnelse == Standardbegrunnelse.ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_ENDRET_UTBETALING) {
            val deltBostedEndringsperioder = this.standardbegrunnelse.hentRelevanteEndringsperioderForBegrunnelse(minimerteRestEndredeAndeler = restBehandlingsgrunnlagForBrev.minimerteEndredeUtbetalingAndeler, vedtaksperiode = periode).filter { it.årsak == Årsak.DELT_BOSTED }
            val deltBostedEndringsperioderGruppertPåAvtaledato = deltBostedEndringsperioder.filter { endringsperiode -> restBehandlingsgrunnlagForBrev.personerPåBehandling.find { person -> person.personIdent == endringsperiode.personIdent }?.type == PersonType.BARN }.groupBy { it.avtaletidspunktDeltBosted }

            return deltBostedEndringsperioderGruppertPåAvtaledato.map {
                BrevBegrunnelseGrunnlagMedPersoner(
                    standardbegrunnelse = this.standardbegrunnelse,
                    vedtakBegrunnelseType = this.standardbegrunnelse.vedtakBegrunnelseType,
                    triggesAv = this.triggesAv,
                    personIdenter = it.value.map { endringsperiode -> endringsperiode.personIdent },
                    avtaletidspunktDeltBosted = it.key
                )
            }
        }

        val personidenterGjeldendeForBegrunnelse: Set<String> = hentPersonidenterGjeldendeForBegrunnelse(
            triggesAv = this.triggesAv,
            vedtakBegrunnelseType = this.standardbegrunnelse.vedtakBegrunnelseType,
            periode = periode,
            vedtaksperiodetype = vedtaksperiodetype,
            restBehandlingsgrunnlagForBrev = restBehandlingsgrunnlagForBrev,
            identerMedUtbetalingPåPeriode = identerMedUtbetalingPåPeriode,
            erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak,
            identerMedReduksjonPåPeriode = barnPersonIdentMedReduksjon.map { it },
            erIngenOverlappVedtaksperiodeTogglePå = erIngenOverlappVedtaksperiodeTogglePå,
            minimerteUtbetalingsperiodeDetaljer = minimerteUtbetalingsperiodeDetaljer,
        )

        if (
            personidenterGjeldendeForBegrunnelse.isEmpty() &&
            !erUregistrerteBarnPåbehandling &&
            !this.triggesAv.satsendring
        ) {
            throw Feil(
                "Begrunnelse '${this.standardbegrunnelse}' var ikke knyttet til noen personer."
            )
        }

        return listOf(
            BrevBegrunnelseGrunnlagMedPersoner(
                standardbegrunnelse = this.standardbegrunnelse,
                vedtakBegrunnelseType = this.standardbegrunnelse.vedtakBegrunnelseType,
                triggesAv = this.triggesAv,
                personIdenter = personidenterGjeldendeForBegrunnelse.toList()
            )
        )
    }

    fun tilBrevBegrunnelseGrunnlagForLogging() = BrevBegrunnelseGrunnlagForLogging(
        standardbegrunnelse = this.standardbegrunnelse,
    )
}

fun RestVedtaksbegrunnelse.tilBrevBegrunnelseGrunnlag(
    sanityBegrunnelser: List<SanityBegrunnelse>
): BrevBegrunnelseGrunnlag {
    return BrevBegrunnelseGrunnlag(
        standardbegrunnelse = this.standardbegrunnelse,
        triggesAv = sanityBegrunnelser
            .firstOrNull { it.apiNavn == this.standardbegrunnelse.sanityApiNavn }!!
            .tilTriggesAv()
    )
}
