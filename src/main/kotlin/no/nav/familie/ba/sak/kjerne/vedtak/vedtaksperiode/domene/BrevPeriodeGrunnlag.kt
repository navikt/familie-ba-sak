package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene

import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.NullablePeriode
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.dokument.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.MinimertPerson
import no.nav.familie.ba.sak.kjerne.vedtak.domene.tilMinimertPerson
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.UtbetalingsperiodeDetalj
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import java.math.BigDecimal
import java.time.LocalDate

data class BrevPeriodeGrunnlag(
    val fom: LocalDate?,
    val tom: LocalDate?,
    val type: Vedtaksperiodetype,
    val begrunnelser: List<BrevBegrunnelseGrunnlag>,
    val fritekster: List<String> = emptyList(),
    val minimerteUtbetalingsperiodeDetaljer: List<MinimertUtbetalingsperiodeDetalj> = emptyList(),
) {
    fun tilBrevPeriodeGrunnlagMedPersoner(
        brevGrunnlag: BrevGrunnlag,
        erFørsteVedtaksperiodePåFagsak: Boolean
    ): BrevPeriodeGrunnlagMedPersoner {
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
                    brevGrunnlag = brevGrunnlag,
                    identerMedUtbetaling = this.minimerteUtbetalingsperiodeDetaljer
                        .map { utbetalingsperiodeDetalj -> utbetalingsperiodeDetalj.person.personIdent },
                    erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak
                )
            },
            fritekster = this.fritekster,
            minimerteUtbetalingsperiodeDetaljer = this.minimerteUtbetalingsperiodeDetaljer,
            erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak,
        )
    }

    fun hentMånedPeriode() = MånedPeriode(
        (fom ?: TIDENES_MORGEN).toYearMonth(),
        (tom ?: TIDENES_ENDE).toYearMonth()
    )
}

data class MinimertUtbetalingsperiodeDetalj(
    val person: MinimertPerson,
    val ytelseType: YtelseType,
    val utbetaltPerMnd: Int,
    val erPåvirketAvEndring: Boolean,
    val prosent: BigDecimal,
)

fun UtbetalingsperiodeDetalj.tilMinimertUtbetalingsperiodeDetalj() = MinimertUtbetalingsperiodeDetalj(
    person = this.person.tilMinimertPerson(),
    ytelseType = this.ytelseType,
    utbetaltPerMnd = this.utbetaltPerMnd,
    erPåvirketAvEndring = this.erPåvirketAvEndring,
    prosent = this.prosent
)

fun UtvidetVedtaksperiodeMedBegrunnelser.tilBrevPeriodeGrunnlag(
    sanityBegrunnelser: List<SanityBegrunnelse>
): BrevPeriodeGrunnlag {
    return BrevPeriodeGrunnlag(
        fom = this.fom,
        tom = this.tom,
        type = this.type,
        fritekster = this.fritekster,
        minimerteUtbetalingsperiodeDetaljer = this.utbetalingsperiodeDetaljer.map { it.tilMinimertUtbetalingsperiodeDetalj() },
        begrunnelser = this.begrunnelser.map { it.tilBrevBegrunnelseGrunnlag(sanityBegrunnelser) }
    )
}


