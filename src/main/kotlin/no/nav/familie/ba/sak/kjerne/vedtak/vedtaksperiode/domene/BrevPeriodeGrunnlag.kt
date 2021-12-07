package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene

import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.NullablePeriode
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.dokument.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.dokument.domene.tilTriggesAv
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.TriggesAv
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.BegrunnelseGrunnlag
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.RestVedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.UtbetalingsperiodeDetalj
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.hentPersonidenterGjeldendeForBegrunnelse
import java.time.LocalDate

data class BrevPeriodeGrunnlag(
    val fom: LocalDate?,
    val tom: LocalDate?,
    val type: Vedtaksperiodetype,
    val begrunnelser: List<BrevBegrunnelseGrunnlag>,
    val fritekster: List<String> = emptyList(),
    val utbetalingsperiodeDetaljer: List<UtbetalingsperiodeDetalj> = emptyList(),
) {
    fun tilBrevPeriodeGrunnlagMedPersoner(begrunnelseGrunnlag: BegrunnelseGrunnlag): BrevPeriodeGrunnlagMedPersoner {
        return BrevPeriodeGrunnlagMedPersoner(
            fom = this.fom,
            tom = this.tom,
            type = this.type,
            begrunnelser = this.begrunnelser.map {
                it.tilBrevBegrunnelseGrunnlagMedPersoner(
                    periode = NullablePeriode(
                        fom = this.fom,
                        tom = this.tom
                    ),
                    periodeType = this.type,
                    begrunnelseGrunnlag = begrunnelseGrunnlag
                )
            },
            fritekster = this.fritekster,
            utbetalingsperiodeDetaljer = this.utbetalingsperiodeDetaljer
        )
    }

    fun hentMånedPeriode() = MånedPeriode(
        (fom ?: TIDENES_MORGEN).toYearMonth(),
        (tom ?: TIDENES_ENDE).toYearMonth()
    )
}

fun UtvidetVedtaksperiodeMedBegrunnelser.tilBrevPeriodeGrunnlag(
    sanityBegrunnelser: List<SanityBegrunnelse>
): BrevPeriodeGrunnlag {
    return BrevPeriodeGrunnlag(
        fom = this.fom,
        tom = this.tom,
        type = this.type,
        fritekster = this.fritekster,
        utbetalingsperiodeDetaljer = this.utbetalingsperiodeDetaljer,
        begrunnelser = this.begrunnelser.map { it.tilBrevBegrunnelseGrunnlag(sanityBegrunnelser) }
    )
}

data class BrevBegrunnelseGrunnlag(
    val vedtakBegrunnelseSpesifikasjon: VedtakBegrunnelseSpesifikasjon,
    val vedtakBegrunnelseType: VedtakBegrunnelseType,
    val triggesAv: TriggesAv,
) {
    fun tilBrevBegrunnelseGrunnlagMedPersoner(
        periode: NullablePeriode,
        periodeType: Vedtaksperiodetype,
        begrunnelseGrunnlag: BegrunnelseGrunnlag
    ): BrevBegrunnelseGrunnlagMedPersoner {
        val personidenterGjeldendeForBegrunnelse: List<String> = hentPersonidenterGjeldendeForBegrunnelse(
            triggesAv = this.triggesAv,
            vedtakBegrunnelseType = this.vedtakBegrunnelseType,
            periode = periode,
            vedtaksperiodeType = periodeType,
            begrunnelseGrunnlag = begrunnelseGrunnlag
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
            .firstOrNull { it.apiNavn == this.vedtakBegrunnelseSpesifikasjon.sanityApiNavn }!! // TODO : Håndtere feil
            .tilTriggesAv()
    )
}

data class BrevPeriodeGrunnlagMedPersoner(
    val fom: LocalDate?,
    val tom: LocalDate?,
    val type: Vedtaksperiodetype,
    val begrunnelser: List<BrevBegrunnelseGrunnlagMedPersoner>,
    val fritekster: List<String> = emptyList(),
    val utbetalingsperiodeDetaljer: List<UtbetalingsperiodeDetalj> = emptyList(),
)

data class BrevBegrunnelseGrunnlagMedPersoner(
    val vedtakBegrunnelseSpesifikasjon: VedtakBegrunnelseSpesifikasjon,
    val vedtakBegrunnelseType: VedtakBegrunnelseType,
    val triggesAv: TriggesAv,
    val personIdenter: List<String> = emptyList(),
)
