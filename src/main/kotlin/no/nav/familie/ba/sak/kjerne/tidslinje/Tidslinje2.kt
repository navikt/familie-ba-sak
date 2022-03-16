package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Periode
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidspunkt
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.fpsak.tidsserie.StandardCombinators
import java.time.LocalDate

class Tidslinje2<T>(val perioder: List<Periode<T>>) {

    fun <U, R> kombinerMed(tidslinje: Tidslinje2<U>, kombinator: PeriodeKombinator<T, U, R>): Tidslinje2<R> {
        val sammenlagt =
            this.toLocalDateTimeline().combine(
                tidslinje.toLocalDateTimeline(),
                kombinator,
                LocalDateTimeline.JoinStyle.CROSS_JOIN
            ).tilTidslinje()

        return sammenlagt
    }
}

interface PeriodeKombinator<T, U, R> : LocalDateSegmentCombinator<T, U, R>

fun <T> Tidslinje2<T>.toLocalDateTimeline(): LocalDateTimeline<T> {
    return LocalDateTimeline(
        this.perioder.sortedBy { it.fom }.map { it.tilSegment() }
    )
}

fun <T> Periode<T>.tilSegment(): LocalDateSegment<T> =
    LocalDateSegment(
        this.fom.tilFørsteDagIMåneden().tilLocalDate(),
        this.tom.tilSisteDagIMåneden().tilLocalDate(),
        this.innhold
    )

fun <T> LocalDateTimeline<T>.tilTidslinje(): Tidslinje2<T> {
    val perioder = this.compress().toSegments().map { it.tilPeriode() }
    return Tidslinje2(perioder)
}

fun <T> LocalDateSegment<T>.tilPeriode(): Periode<T> =
    Periode(
        fom = Tidspunkt(this.fom),
        tom = Tidspunkt(this.tom),
        innhold = this.value
    )

fun <T, R> Iterable<Tidslinje2<T>>.kombiner(listeKombinator: ListeKombinator<T, R>): Tidslinje2<R> {
    val startVerdi = LocalDateTimeline(LocalDate.MIN, LocalDate.MAX, emptyList<T>())

    return this.map { it.toLocalDateTimeline() }
        .fold(startVerdi) { acc, neste -> kombinerVerdier(acc, neste) }
        .toSegments()
        .map { LocalDateSegment(it.fom, it.tom, listeKombinator.kombiner(it.value)) }
        .map { it.tilPeriode() }
        .let { Tidslinje2(it) }
}

private fun <T> kombinerVerdier(
    lhs: LocalDateTimeline<List<T>>,
    rhs: LocalDateTimeline<T>
): LocalDateTimeline<List<T>> {
    return lhs.combine(
        rhs,
        { datoIntervall, sammenlagt, neste ->
            StandardCombinators.allValues(
                datoIntervall,
                sammenlagt,
                neste
            )
        },
        LocalDateTimeline.JoinStyle.CROSS_JOIN
    )
}

interface ListeKombinator<T, R> {
    fun kombiner(liste: Iterable<T>): R
}
