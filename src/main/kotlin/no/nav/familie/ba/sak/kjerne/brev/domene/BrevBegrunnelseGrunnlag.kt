package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.NullablePeriode
import no.nav.familie.ba.sak.kjerne.brev.hentPersonidenterGjeldendeForBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.TriggesAv
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.RestVedtaksbegrunnelse

data class BrevBegrunnelseGrunnlag(
    val vedtakBegrunnelseSpesifikasjon: VedtakBegrunnelseSpesifikasjon,
    val triggesAv: TriggesAv,
) {
    fun tilBrevBegrunnelseGrunnlagMedPersoner(
        periode: NullablePeriode,
        vedtaksperiodetype: Vedtaksperiodetype,
        restBehandlingsgrunnlagForBrev: RestBehandlingsgrunnlagForBrev,
        identerMedUtbetalingPåPeriode: List<String>,
        erFørsteVedtaksperiodePåFagsak: Boolean,
        erUregistrerteBarnPåbehandling: Boolean,
        barnPersonIdentMedReduksjon: List<String>
    ): BrevBegrunnelseGrunnlagMedPersoner {
        val personidenterGjeldendeForBegrunnelse: Set<String> = hentPersonidenterGjeldendeForBegrunnelse(
            triggesAv = this.triggesAv,
            vedtakBegrunnelseType = this.vedtakBegrunnelseSpesifikasjon.vedtakBegrunnelseType,
            periode = periode,
            vedtaksperiodetype = vedtaksperiodetype,
            restBehandlingsgrunnlagForBrev = restBehandlingsgrunnlagForBrev,
            identerMedUtbetalingPåPeriode = identerMedUtbetalingPåPeriode,
            erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak,
            identerMedReduksjonPåPeriode = barnPersonIdentMedReduksjon.map { it }
        )

        if (
            personidenterGjeldendeForBegrunnelse.isEmpty() &&
            !erUregistrerteBarnPåbehandling &&
            !this.triggesAv.satsendring
        ) {
            throw Feil(
                "Begrunnelse '${this.vedtakBegrunnelseSpesifikasjon}' var ikke knyttet til noen personer."
            )
        }

        return BrevBegrunnelseGrunnlagMedPersoner(
            vedtakBegrunnelseSpesifikasjon = this.vedtakBegrunnelseSpesifikasjon,
            vedtakBegrunnelseType = this.vedtakBegrunnelseSpesifikasjon.vedtakBegrunnelseType,
            triggesAv = this.triggesAv,
            personIdenter = personidenterGjeldendeForBegrunnelse.toList()
        )
    }

    fun tilBrevBegrunnelseGrunnlagForLogging() = BrevBegrunnelseGrunnlagForLogging(
        vedtakBegrunnelseSpesifikasjon = this.vedtakBegrunnelseSpesifikasjon,
    )
}

fun RestVedtaksbegrunnelse.tilBrevBegrunnelseGrunnlag(
    sanityBegrunnelser: List<SanityBegrunnelse>
): BrevBegrunnelseGrunnlag {
    return BrevBegrunnelseGrunnlag(
        vedtakBegrunnelseSpesifikasjon = this.vedtakBegrunnelseSpesifikasjon,
        triggesAv = sanityBegrunnelser
            .firstOrNull { it.apiNavn == this.vedtakBegrunnelseSpesifikasjon.sanityApiNavn }!!
            .tilTriggesAv()
    )
}
