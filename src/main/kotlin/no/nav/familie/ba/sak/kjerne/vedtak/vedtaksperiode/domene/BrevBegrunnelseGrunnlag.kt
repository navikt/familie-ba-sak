package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene

import no.nav.familie.ba.sak.common.NullablePeriode
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.tilTriggesAv
import no.nav.familie.ba.sak.kjerne.brev.hentPersonidenterGjeldendeForBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.TriggesAv
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype

data class BrevBegrunnelseGrunnlag(
    val vedtakBegrunnelseSpesifikasjon: VedtakBegrunnelseSpesifikasjon,
    val vedtakBegrunnelseType: VedtakBegrunnelseType,
    val triggesAv: TriggesAv,
) {
    fun tilBrevBegrunnelseGrunnlagMedPersoner(
        periode: NullablePeriode,
        periodeType: Vedtaksperiodetype,
        brevGrunnlag: BrevGrunnlag,
        identerMedUtbetaling: List<String>,
        erFørsteVedtaksperiodePåFagsak: Boolean,
    ): BrevBegrunnelseGrunnlagMedPersoner {
        val personidenterGjeldendeForBegrunnelse: List<String> = hentPersonidenterGjeldendeForBegrunnelse(
            triggesAv = this.triggesAv,
            vedtakBegrunnelseType = this.vedtakBegrunnelseType,
            periode = periode,
            vedtaksperiodeType = periodeType,
            brevGrunnlag = brevGrunnlag,
            identerMedUtbetaling = identerMedUtbetaling,
            erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak,
        )

        return BrevBegrunnelseGrunnlagMedPersoner(
            vedtakBegrunnelseSpesifikasjon = this.vedtakBegrunnelseSpesifikasjon,
            vedtakBegrunnelseType = this.vedtakBegrunnelseType,
            triggesAv = this.triggesAv,
            personIdenter = personidenterGjeldendeForBegrunnelse
        )
    }
}

fun RestVedtaksbegrunnelse.tilBrevBegrunnelseGrunnlag(
    sanityBegrunnelser: List<SanityBegrunnelse>
): BrevBegrunnelseGrunnlag {
    return BrevBegrunnelseGrunnlag(
        vedtakBegrunnelseSpesifikasjon = this.vedtakBegrunnelseSpesifikasjon,
        vedtakBegrunnelseType = this.vedtakBegrunnelseType,
        triggesAv = sanityBegrunnelser
            .firstOrNull { it.apiNavn == this.vedtakBegrunnelseSpesifikasjon.sanityApiNavn }!!
            .tilTriggesAv()
    )
}
