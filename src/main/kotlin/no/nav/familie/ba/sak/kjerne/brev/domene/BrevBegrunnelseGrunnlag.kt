package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.NullablePeriode
import no.nav.familie.ba.sak.kjerne.brev.hentPersonidenterGjeldendeForBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.TriggesAv
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.delOpp
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
                vedtaksperiodetype = vedtaksperiodetype,
                restBehandlingsgrunnlagForBrev = restBehandlingsgrunnlagForBrev,
                identerMedUtbetalingPåPeriode = identerMedUtbetalingPåPeriode,
                erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak,
                identerMedReduksjonPåPeriode = barnPersonIdentMedReduksjon.map { it },
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
