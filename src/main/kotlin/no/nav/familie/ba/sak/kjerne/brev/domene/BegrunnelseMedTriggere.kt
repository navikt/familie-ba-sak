package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.NullablePeriode
import no.nav.familie.ba.sak.kjerne.brev.hentPersonidenterGjeldendeForBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.TriggesAv
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype

data class BegrunnelseMedTriggere(
    val standardbegrunnelse: IVedtakBegrunnelse,
    val triggesAv: TriggesAv
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
        dødeBarnForrigePeriode: List<String>
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
                identerMedReduksjonPåPeriode = barnMedReduksjonFraForrigeBehandlingIdent,
                minimerteUtbetalingsperiodeDetaljer = minimerteUtbetalingsperiodeDetaljer,
                dødeBarnForrigePeriode = dødeBarnForrigePeriode
            )

            if (
                personidenterGjeldendeForBegrunnelse.isEmpty() &&
                !erUregistrerteBarnPåbehandling &&
                !this.triggesAv.satsendring
            ) {
                throw FunksjonellFeil(
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
        standardbegrunnelse = this.standardbegrunnelse
    )
}

fun Vedtaksbegrunnelse.tilBegrunnelseMedTriggere(
    sanityBegrunnelser: List<SanityBegrunnelse>
): BegrunnelseMedTriggere {
    val sanityBegrunnelse = sanityBegrunnelser.firstOrNull { it.apiNavn == this.standardbegrunnelse.sanityApiNavn } ?: throw Feil("Finner ikke sanityBegrunnelse med apiNavn=${this.standardbegrunnelse.sanityApiNavn}")
    return BegrunnelseMedTriggere(
        standardbegrunnelse = this.standardbegrunnelse,
        triggesAv = sanityBegrunnelse
            .tilTriggesAv()
    )
}
