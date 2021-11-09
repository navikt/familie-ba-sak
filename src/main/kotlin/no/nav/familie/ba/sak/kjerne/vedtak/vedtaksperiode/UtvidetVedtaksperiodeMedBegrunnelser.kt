package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIForrigeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
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
    val utbetalingsperiodeDetaljer: List<UtbetalingsperiodeDetalj> = emptyList(),
) {
    fun hentMånedPeriode() = MånedPeriode(
        (fom ?: TIDENES_MORGEN).toYearMonth(),
        (tom ?: TIDENES_ENDE).toYearMonth()
    )

    fun inneholderSammeBegrunnelserOgPersonerSom(
        utvidetVedtaksperiodeMedBegrunnelser: UtvidetVedtaksperiodeMedBegrunnelser
    ) =
        this.begrunnelser.containsAll(utvidetVedtaksperiodeMedBegrunnelser.begrunnelser) &&
            this.hentPersonIdenter()
                .containsAll(utvidetVedtaksperiodeMedBegrunnelser.hentPersonIdenter())

    fun hentPersonIdenter() = utbetalingsperiodeDetaljer.map { it.person.personIdent }
}

data class RestVedtaksbegrunnelse(
    val vedtakBegrunnelseSpesifikasjon: VedtakBegrunnelseSpesifikasjon,
    val vedtakBegrunnelseType: VedtakBegrunnelseType,
    val personIdenter: List<String> = emptyList(),
)

fun List<UtvidetVedtaksperiodeMedBegrunnelser>.sorter(): List<UtvidetVedtaksperiodeMedBegrunnelser> {
    val (perioderMedFom, perioderUtenFom) = this.partition { it.fom != null }
    return perioderMedFom.sortedWith(compareBy { it.fom }) + perioderUtenFom
}

fun VedtaksperiodeMedBegrunnelser.tilUtvidetVedtaksperiodeMedBegrunnelser(
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>
): UtvidetVedtaksperiodeMedBegrunnelser {

    val utbetalingsperiodeDetaljer =
        if (this.type == Vedtaksperiodetype.UTBETALING ||
            this.type == Vedtaksperiodetype.ENDRET_UTBETALING ||
            this.type == Vedtaksperiodetype.FORTSATT_INNVILGET
        ) {
            val andelerForVedtaksperiodetype = andelerTilkjentYtelse.filter {
                if (this.type == Vedtaksperiodetype.ENDRET_UTBETALING) {
                    endretUtbetalingAndelSkalVæreMed(it)
                } else {
                    it.endretUtbetalingAndeler.isEmpty()
                }
            }
            val vertikaltSegmentForVedtaksperiode =
                if (this.type == Vedtaksperiodetype.FORTSATT_INNVILGET)
                    hentLøpendeAndelForVedtaksperiode(andelerForVedtaksperiodetype)
                else
                    hentVertikaltSegmentForVedtaksperiode(andelerForVedtaksperiodetype)

            val andelerForSegment =
                hentAndelerForSegment(andelerForVedtaksperiodetype, vertikaltSegmentForVedtaksperiode)

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

private fun VedtaksperiodeMedBegrunnelser.endretUtbetalingAndelSkalVæreMed(andelTilkjentYtelse: AndelTilkjentYtelse) =
    andelTilkjentYtelse.endretUtbetalingAndeler.isNotEmpty() && andelTilkjentYtelse.endretUtbetalingAndeler.all { endretUtbetalingAndel ->
        this.begrunnelser.any { vedtaksbegrunnelse ->
            vedtaksbegrunnelse.personIdenter.contains(
                endretUtbetalingAndel.person!!.personIdent.ident
            )
        }
    }

private fun VedtaksperiodeMedBegrunnelser.hentLøpendeAndelForVedtaksperiode(andelerTilkjentYtelse: List<AndelTilkjentYtelse>): LocalDateSegment<Int> {
    val sorterteSegmenter = andelerTilkjentYtelse.utledSegmenter().sortedBy { it.fom }
    return sorterteSegmenter.lastOrNull { it.fom.toYearMonth() <= inneværendeMåned() }
        ?: sorterteSegmenter.firstOrNull()
        ?: throw Feil("Finner ikke gjeldende segment ved fortsatt innvilget")
}

private fun VedtaksperiodeMedBegrunnelser.hentVertikaltSegmentForVedtaksperiode(
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>
) = andelerTilkjentYtelse
    .utledSegmenter()
    .find { localDateSegment ->
        localDateSegment.fom == this.fom || localDateSegment.tom == this.tom
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

fun List<UtvidetVedtaksperiodeMedBegrunnelser>.slåSammenEtterfølgendeEndringsperioderMedSammeBegrunnelserOgPersoner():
    List<UtvidetVedtaksperiodeMedBegrunnelser> {
    return this
        .filter { it.type == Vedtaksperiodetype.ENDRET_UTBETALING }
        .sorter()
        .fold(emptyList<UtvidetVedtaksperiodeMedBegrunnelser>()) { acc, utvidetVedtaksperiodeMedBegrunnelser ->
            val forrigeEndringsperiode = acc.lastOrNull()
            if (
                forrigeEndringsperiode != null &&
                forrigeEndringsperiode.inneholderSammeBegrunnelserOgPersonerSom(utvidetVedtaksperiodeMedBegrunnelser) &&
                forrigeEndringsperiode.tom == utvidetVedtaksperiodeMedBegrunnelser.fom?.sisteDagIForrigeMåned()
            )
                acc.filter { forrigeEndringsperiode.id != it.id } +
                    forrigeEndringsperiode.copy(tom = utvidetVedtaksperiodeMedBegrunnelser.tom)
            else
                acc + utvidetVedtaksperiodeMedBegrunnelser
        } + this.filter { it.type != Vedtaksperiodetype.ENDRET_UTBETALING }
}
