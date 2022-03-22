package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TidslinjeMedAvhengigheter
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.komprimer
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.fpsak.tidsserie.LocalDateInterval
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.fpsak.tidsserie.StandardCombinators

class LocalDatetimeTimelineToveisTidslinje<V, H, R, T : Tidsenhet>(
    val venstre: Tidslinje<V, T>,
    val høyre: Tidslinje<H, T>,
    val periodeKombinator: ToveisKombinator<V, H, R>,
) : TidslinjeMedAvhengigheter<R, T>(listOf(venstre, høyre)) {

    override fun lagPerioder(): Collection<Periode<R, T>> {

        return venstre.toLocalDateTimeline().combine(
            høyre.toLocalDateTimeline(),
            LocalDateSegmentPeriodeKombinator(periodeKombinator),
            LocalDateTimeline.JoinStyle.CROSS_JOIN
        ).compress()
            .toSegments()
            .map { it.tilPeriode(venstre.perioder().first().fraOgMed) }
    }
}

class LocalDatetimeTimelineListeTidslinje<I, R, T : Tidsenhet>(
    val tidslinjer: Collection<Tidslinje<I, T>>,
    val listeKombinator: ListeKombinator<I, R>,
) : TidslinjeMedAvhengigheter<R, T>(tidslinjer) {

    override fun lagPerioder(): Collection<Periode<R, T>> {
        val startVerdi = LocalDateTimeline(
            fraOgMed().tilLocalDateEllerNull(),
            tilOgMed().tilLocalDateEllerNull(),
            emptyList<I>()
        )

        return tidslinjer.map { it.toLocalDateTimeline() }
            .fold(startVerdi) { acc, neste -> kombinerVerdier(acc, neste) }
            .compress()
            .toSegments()
            .map { LocalDateSegment(it.fom, it.tom, listeKombinator.kombiner(it.value)) }
            .map { it.tilPeriode(tidslinjer.first().perioder().first().fraOgMed) }
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

fun <I, R, T : Tidsenhet> Collection<Tidslinje<I, T>>.kombiner(listeKombinator: ListeKombinator<I, R>): Tidslinje<R, T> {
    // Har ikke fått LocalDateTimeline.compress til å funke helt, og kjører egen komprimering på toppen
    return LocalDatetimeTimelineListeTidslinje(this, listeKombinator).komprimer()
}

fun <V, H, R, T : Tidsenhet> Tidslinje<V, T>.kombinerMed(
    tidslinje: Tidslinje<H, T>,
    kombinator: ToveisKombinator<V, H, R>
): Tidslinje<R, T> {
    // Har ikke fått LocalDateTimeline.compress til å funke helt, og kjører egen komprimering på toppen
    return LocalDatetimeTimelineToveisTidslinje(this, tidslinje, kombinator).komprimer()
}

fun <I, T : Tidsenhet> Tidslinje<I, T>.toLocalDateTimeline(): LocalDateTimeline<I> {
    return LocalDateTimeline(
        this.perioder().map { it.tilLocalDateSegment() }
    )
}

fun <I, T : Tidsenhet> Periode<I, T>.tilLocalDateSegment(): LocalDateSegment<I> =
    LocalDateSegment(
        this.fraOgMed.tilFørsteDagIMåneden().tilLocalDateEllerNull(),
        this.tilOgMed.tilSisteDagIMåneden().tilLocalDateEllerNull(),
        this.innhold
    )

fun <I, T : Tidsenhet> LocalDateSegment<I>.tilPeriode(tidspunktMal: Tidspunkt<T>): Periode<I, T> =
    Periode(
        fraOgMed = tidspunktMal.somFraOgMed(this.fom),
        tilOgMed = tidspunktMal.somTilOgMed(this.tom),
        innhold = this.value
    )
