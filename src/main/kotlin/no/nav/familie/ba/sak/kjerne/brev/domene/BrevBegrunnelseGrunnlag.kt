package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.NullablePeriode
import no.nav.familie.ba.sak.kjerne.brev.hentPersonidenterGjeldendeForBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.TriggesAv
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.delOpp
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.RestVedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår

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
        barnMedReduksjonFraForrigeBehandlingIdent: List<String>,
        minimerteUtbetalingsperiodeDetaljer: List<MinimertUtbetalingsperiodeDetalj>,
    ): List<BrevBegrunnelseGrunnlagMedPersoner> {

        return if (this.standardbegrunnelse.kanDelesOpp) {
            this.standardbegrunnelse.delOpp(
                restBehandlingsgrunnlagForBrev = restBehandlingsgrunnlagForBrev,
                triggesAv = this.triggesAv,
                periode = periode
            )
        } else {
            val personidenterGjeldendeForBegrunnelse: Set<String> = hentPersonidenterGjeldendeForBegrunnelse(
                triggesAv = this.triggesAv,
                vedtakBegrunnelseType = this.standardbegrunnelse.vedtakBegrunnelseType,
                periode = periode,
                restBehandlingsgrunnlagForBrev = restBehandlingsgrunnlagForBrev,
                identerMedUtbetalingPåPeriode = identerMedUtbetalingPåPeriode,
                erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak,
                minimerteUtbetalingsperiodeDetaljer = minimerteUtbetalingsperiodeDetaljer,
            ) + hentPersonidenterMedReduksjonFraForrigeBehandlingForBegrunnelse(
                triggesAv = this.triggesAv,
                vedtakBegrunnelseType = this.standardbegrunnelse.vedtakBegrunnelseType,
                vedtaksperiodetype = vedtaksperiodetype,
                identerMedReduksjonPåPeriode = barnMedReduksjonFraForrigeBehandlingIdent
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

            listOf(
                BrevBegrunnelseGrunnlagMedPersoner(
                    standardbegrunnelse = this.standardbegrunnelse,
                    vedtakBegrunnelseType = this.standardbegrunnelse.vedtakBegrunnelseType,
                    triggesAv = this.triggesAv,
                    personIdenter = personidenterGjeldendeForBegrunnelse.toList()
                )
            )
        }
    }

    fun tilBrevBegrunnelseGrunnlagForLogging() = BrevBegrunnelseGrunnlagForLogging(
        standardbegrunnelse = this.standardbegrunnelse,
    )
}

private fun hentPersonidenterMedReduksjonFraForrigeBehandlingForBegrunnelse(
    identerMedReduksjonPåPeriode: List<String>,
    vedtakBegrunnelseType: VedtakBegrunnelseType,
    vedtaksperiodetype: Vedtaksperiodetype,
    triggesAv: TriggesAv
): List<String> {
    return if (vedtaksperiodetype == Vedtaksperiodetype.UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING && vedtakBegrunnelseType == VedtakBegrunnelseType.REDUKSJON && !triggesAv.vilkår.contains(
            Vilkår.UNDER_18_ÅR
        )
    )
        identerMedReduksjonPåPeriode
    else emptyList()
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
