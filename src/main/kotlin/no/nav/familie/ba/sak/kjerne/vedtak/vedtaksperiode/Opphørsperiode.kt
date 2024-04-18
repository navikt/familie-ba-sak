package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombiner
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tilTidslinje
import java.time.LocalDate

data class Opphørsperiode(
    override val periodeFom: LocalDate,
    override val periodeTom: LocalDate?,
    override val vedtaksperiodetype: Vedtaksperiodetype = Vedtaksperiodetype.OPPHØR,
) : Vedtaksperiode


fun List<AndelTilkjentYtelseMedEndreteUtbetalinger>.tilKombinertTidslinjePerAktørOgType(): Tidslinje<Collection<AndelTilkjentYtelseMedEndreteUtbetalinger>, Måned> {
    val andelTilkjentYtelsePerPersonOgType = groupBy { Pair(it.aktør, it.type) }

    val andelTilkjentYtelsePerPersonOgTypeTidslinjer =
        andelTilkjentYtelsePerPersonOgType.values.map { it.tilTidslinje() }

    return andelTilkjentYtelsePerPersonOgTypeTidslinjer.kombiner { it.toList() }
}

@JvmName("AndelTilkjentYtelseMedEndreteUtbetalingerTilTidslinje")
fun List<AndelTilkjentYtelseMedEndreteUtbetalinger>.tilTidslinje(): Tidslinje<AndelTilkjentYtelseMedEndreteUtbetalinger, Måned> =
    this.map {
        Periode(
            fraOgMed = it.stønadFom.tilTidspunkt(),
            tilOgMed = it.stønadTom.tilTidspunkt(),
            innhold = it,
        )
    }.tilTidslinje()

fun slåSammenOpphørsperioder(alleOpphørsperioder: List<Opphørsperiode>): List<Opphørsperiode> {
    if (alleOpphørsperioder.isEmpty()) return emptyList()

    val sortertOpphørsperioder = alleOpphørsperioder.sortedBy { it.periodeFom }

    return sortertOpphørsperioder.fold(
        mutableListOf(
            sortertOpphørsperioder.first(),
        ),
    ) { acc: MutableList<Opphørsperiode>, nesteOpphørsperiode: Opphørsperiode ->
        val forrigeOpphørsperiode = acc.last()
        when {
            nesteOpphørsperiode.periodeFom.isSameOrBefore(forrigeOpphørsperiode.periodeTom ?: TIDENES_ENDE) -> {
                acc[acc.lastIndex] =
                    forrigeOpphørsperiode.copy(
                        periodeTom =
                            maxOfOpphørsperiodeTom(
                                forrigeOpphørsperiode.periodeTom,
                                nesteOpphørsperiode.periodeTom,
                            ),
                    )
            }

            else -> {
                acc.add(nesteOpphørsperiode)
            }
        }

        acc
    }
}

private fun maxOfOpphørsperiodeTom(
    a: LocalDate?,
    b: LocalDate?,
): LocalDate? {
    return if (a != null && b != null) maxOf(a, b) else null
}
