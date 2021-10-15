package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.domene.tilRestVedtaksbegrunnelse
import no.nav.fpsak.tidsserie.LocalDateInterval
import no.nav.fpsak.tidsserie.LocalDateSegment
import java.time.LocalDate

data class UtvidetVedtaksperiodeMedBegrunnelser(
    val id: Long,
    val fom: LocalDate?,
    val tom: LocalDate?,
    val type: Vedtaksperiodetype,
    val begrunnelser: List<RestVedtaksbegrunnelse>,
    val fritekster: List<String> = emptyList(),
    val gyldigeBegrunnelser: List<VedtakBegrunnelseSpesifikasjon> = emptyList(),
    val utbetalingsperiodeDetaljer: List<UtbetalingsperiodeDetalj>,
)

data class RestVedtaksbegrunnelse(
    val vedtakBegrunnelseSpesifikasjon: VedtakBegrunnelseSpesifikasjon,
    val vedtakBegrunnelseType: VedtakBegrunnelseType,
    val personIdenter: List<String> = emptyList(),
)

fun VedtaksperiodeMedBegrunnelser.tilUtvidetVedtaksperiodeMedBegrunnelser(
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>
): UtvidetVedtaksperiodeMedBegrunnelser {

    val utbetalingsperiodeDetaljer =
        if (this.type == Vedtaksperiodetype.UTBETALING || this.type == Vedtaksperiodetype.ENDRET_UTBETALING) {
            val vertikaltSegmentForVedtaksperiode = hentVertikaltSegmentForVedtaksperiode(andelerTilkjentYtelse)

            val andelerForSegment = hentAndelerForSegment(andelerTilkjentYtelse, vertikaltSegmentForVedtaksperiode)

            andelerForSegment.lagUtbetalingsperiodeDetaljer(personopplysningGrunnlag)
        } else {
            emptyList()
        }


    return UtvidetVedtaksperiodeMedBegrunnelser(
        id = this.id,
        fom = this.fom,
        tom = this.tom,
        type = this.type,
        begrunnelser = this.begrunnelser.map { it.tilRestVedtaksbegrunnelse() },
        fritekster = this.fritekster.sortedBy { it.id }.map { it.fritekst },
        utbetalingsperiodeDetaljer = utbetalingsperiodeDetaljer
    )
}

private fun VedtaksperiodeMedBegrunnelser.hentVertikaltSegmentForVedtaksperiode(
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>
) = andelerTilkjentYtelse
    .utledSegmenter()
    .find { localDateSegment ->
        localDateSegment.fom == this.fom && localDateSegment.tom == this.tom
    } ?: throw Feil("Finner ikke segment for vedtaksperiode (${this.fom}, ${this.tom})")

private fun hentAndelerForSegment(
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    vertikaltSegmentForVedtaksperiode: LocalDateSegment<Int>
) = andelerTilkjentYtelse.filter {
    vertikaltSegmentForVedtaksperiode.localDateInterval.overlaps(
        LocalDateInterval(
            it.stønadFom.førsteDagIInneværendeMåned(),
            it.stønadTom.sisteDagIInneværendeMåned()
        )
    )
}
