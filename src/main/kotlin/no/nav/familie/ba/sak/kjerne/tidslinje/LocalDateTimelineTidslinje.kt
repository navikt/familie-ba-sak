package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.fpsak.tidsserie.LocalDateInterval
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.fpsak.tidsserie.StandardCombinators

interface PeriodeKombinator<V, H, R> {
    fun kombiner(venste: V?, høyre: H?): R
}

interface ListeKombinator<T, R> {
    fun kombiner(liste: Iterable<T>): R
}

class LocalDatetimeTimelineToveisTidslinje<V, H, R>(
    val venstre: Tidslinje<V>,
    val høyre: Tidslinje<H>,
    val periodeKombinator: PeriodeKombinator<V, H, R>
) : TidslinjeMedAvhengigheter<R>(listOf(venstre, høyre)) {

    override fun perioder(): Collection<Periode<R>> {

        return venstre.toLocalDateTimeline().combine(
            høyre.toLocalDateTimeline(),
            LocalDateSegmentPeriodeKombinator(periodeKombinator),
            LocalDateTimeline.JoinStyle.CROSS_JOIN
        ).compress()
            .toSegments()
            .map { it.tilPeriode() }
    }
}

class LocalDatetimeTimelineListeTidslinje<T, R>(
    val tidslinjer: Collection<Tidslinje<T>>,
    val listeKombinator: ListeKombinator<T, R>
) : TidslinjeMedAvhengigheter<R>(tidslinjer) {

    override fun perioder(): Collection<Periode<R>> {
        val startVerdi = LocalDateTimeline(
            tidsrom().start.tilLocalDateEllerNull(),
            tidsrom().endInclusive.tilLocalDateEllerNull(),
            emptyList<T>()
        )

        return tidslinjer.map { it.toLocalDateTimeline() }
            .fold(startVerdi) { acc, neste -> kombinerVerdier(acc, neste) }
            .compress()
            .toSegments()
            .map { LocalDateSegment(it.fom, it.tom, listeKombinator.kombiner(it.value)) }
            .map { it.tilPeriode() }
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
}

class LocalDateSegmentPeriodeKombinator<V, H, R>(val periodeKombinator: PeriodeKombinator<V, H, R>) :
    LocalDateSegmentCombinator<V, H, R> {
    override fun combine(
        intervall: LocalDateInterval?,
        venstre: LocalDateSegment<V>?,
        høyre: LocalDateSegment<H>?
    ): LocalDateSegment<R> {
        return LocalDateSegment(intervall, periodeKombinator.kombiner(venstre?.value, høyre?.value))
    }
}

fun <T, R> Collection<Tidslinje<T>>.kombiner(listeKombinator: ListeKombinator<T, R>): Tidslinje<R> {
    return LocalDatetimeTimelineListeTidslinje(this, listeKombinator).komprimer()
}

fun <T, U, R> Tidslinje<T>.kombinerMed(tidslinje: Tidslinje<U>, kombinator: PeriodeKombinator<T, U, R>): Tidslinje<R> {
    return LocalDatetimeTimelineToveisTidslinje(this, tidslinje, kombinator).komprimer()
}

fun <T> Tidslinje<T>.toLocalDateTimeline(): LocalDateTimeline<T> {
    return LocalDateTimeline(
        this.perioder().sortedBy { it.fom }.map { it.tilLocalDateSegment() }
    )
}

fun <T> Periode<T>.tilLocalDateSegment(): LocalDateSegment<T> =
    LocalDateSegment(
        this.fom.tilFørsteDagIMåneden().tilLocalDateEllerNull(),
        this.tom.tilSisteDagIMåneden().tilLocalDateEllerNull(),
        this.innhold
    )

fun <T> LocalDateSegment<T>.tilPeriode(): Periode<T> =
    Periode(
        fom = Tidspunkt.fraOgMed(this.fom, this.tom),
        tom = Tidspunkt.tilOgMed(this.tom, this.fom),
        innhold = this.value
    )
