package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TidslinjeMedAvhengigheter
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.komprimer
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.fpsak.tidsserie.LocalDateInterval
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.fpsak.tidsserie.StandardCombinators
import java.time.temporal.Temporal

class LocalDatetimeTimelineToveisTidslinje<TID : Temporal, V, H, R>(
    val venstre: Tidslinje<V, TID>,
    val høyre: Tidslinje<H, TID>,
    val periodeKombinator: ToveisKombinator<V, H, R>,
) : TidslinjeMedAvhengigheter<R, TID>(listOf(venstre, høyre)) {

    override fun lagPerioder(): Collection<Periode<R, TID>> {

        return venstre.toLocalDateTimeline().combine(
            høyre.toLocalDateTimeline(),
            LocalDateSegmentPeriodeKombinator(periodeKombinator),
            LocalDateTimeline.JoinStyle.CROSS_JOIN
        ).compress()
            .toSegments()
            .map { it.tilPeriode(venstre.perioder().first().fraOgMed) }
    }
}

class LocalDatetimeTimelineListeTidslinje<TID : Temporal, T, R>(
    val tidslinjer: Collection<Tidslinje<T, TID>>,
    val listeKombinator: ListeKombinator<T, R>,
) : TidslinjeMedAvhengigheter<R, TID>(tidslinjer) {

    override fun lagPerioder(): Collection<Periode<R, TID>> {
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

fun <T, R, TID : Temporal> Collection<Tidslinje<T, TID>>.kombiner(listeKombinator: ListeKombinator<T, R>): Tidslinje<R, TID> {
    // Har ikke fått LocalDateTimeline.compress til å funke helt, og kjører egen komprimering på toppen
    return LocalDatetimeTimelineListeTidslinje(this, listeKombinator).komprimer()
}

fun <T, U, R, TID : Temporal> Tidslinje<T, TID>.kombinerMed(
    tidslinje: Tidslinje<U, TID>,
    kombinator: ToveisKombinator<T, U, R>
): Tidslinje<R, TID> {
    // Har ikke fått LocalDateTimeline.compress til å funke helt, og kjører egen komprimering på toppen
    return LocalDatetimeTimelineToveisTidslinje(this, tidslinje, kombinator).komprimer()
}

fun <T, TID : Temporal> Tidslinje<T, TID>.toLocalDateTimeline(): LocalDateTimeline<T> {
    return LocalDateTimeline(
        this.perioder().map { it.tilLocalDateSegment() }
    )
}

fun <T, TID : Temporal> Periode<T, TID>.tilLocalDateSegment(): LocalDateSegment<T> =
    LocalDateSegment(
        this.fraOgMed.tilFørsteDagIMåneden().tilLocalDateEllerNull(),
        this.tilOgMed.tilSisteDagIMåneden().tilLocalDateEllerNull(),
        this.innhold
    )

fun <T, TID : Temporal> LocalDateSegment<T>.tilPeriode(tidspunktMal: Tidspunkt<TID>): Periode<T, TID> =
    Periode(
        fraOgMed = tidspunktMal.somFraOgMed(this.fom),
        tilOgMed = tidspunktMal.somTilOgMed(this.tom),
        innhold = this.value
    )
