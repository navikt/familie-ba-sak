package no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.fpsak.tidsserie.LocalDateInterval
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.fpsak.tidsserie.StandardCombinators

class LocalDatetimeTimelineToveisTidslinje<V, H, R>(
    val venstre: Tidslinje<V>,
    val høyre: Tidslinje<H>,
    val periodeKombinator: ToveisKombinator<V, H, R>
) : TidslinjeMedAvhengigheter<R>(listOf(venstre, høyre)) {

    override fun lagPerioder(): Collection<Periode<R>> {

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

    override fun lagPerioder(): Collection<Periode<R>> {
        val startVerdi = LocalDateTimeline(
            fraOgMed().tilLocalDateEllerNull(),
            tilOgMed().tilLocalDateEllerNull(),
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

class LocalDateSegmentPeriodeKombinator<V, H, R>(val periodeKombinator: ToveisKombinator<V, H, R>) :
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
    // Har ikke fått LocalDateTimeline.compress til å funke helt, og kjører egen komprimering på toppen
    return LocalDatetimeTimelineListeTidslinje(this, listeKombinator).komprimer()
}

fun <T, U, R> Tidslinje<T>.kombinerMed(tidslinje: Tidslinje<U>, kombinator: ToveisKombinator<T, U, R>): Tidslinje<R> {
    // Har ikke fått LocalDateTimeline.compress til å funke helt, og kjører egen komprimering på toppen
    return LocalDatetimeTimelineToveisTidslinje(this, tidslinje, kombinator).komprimer()
}

fun <T> Tidslinje<T>.toLocalDateTimeline(): LocalDateTimeline<T> {
    return LocalDateTimeline(
        this.perioder().map { it.tilLocalDateSegment() }
    )
}

fun <T> Periode<T>.tilLocalDateSegment(): LocalDateSegment<T> =
    LocalDateSegment(
        this.fraOgMed.tilLocalDateEllerNull(),
        this.tilOgMed.tilLocalDateEllerNull(),
        this.innhold
    )

fun <T> LocalDateSegment<T>.tilPeriode(): Periode<T> =
    Periode(
        fraOgMed = Tidspunkt.fraOgMed(this.fom, this.tom),
        tilOgMed = Tidspunkt.tilOgMed(this.tom, this.fom),
        innhold = this.value
    )
