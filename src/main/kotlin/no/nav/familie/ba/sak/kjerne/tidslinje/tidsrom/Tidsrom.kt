package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.erEndelig
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.erUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.erUendeligLengeTil
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.forrige
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.neste
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.somEndelig
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.somUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.somUendeligLengeTil
import no.nav.familie.ba.sak.kjerne.tidslinje.tidsrom.rangeTo

fun <I, T : Tidsenhet> Tidslinje<I, T>.fraOgMed() =
    this.perioder().firstOrNull()?.fraOgMed

fun <I, T : Tidsenhet> Tidslinje<I, T>.tilOgMed() =
    this.perioder().lastOrNull()?.tilOgMed

fun <T : Tidsenhet> Iterable<Tidslinje<*, T>>.fraOgMed() = this
    .map { it.fraOgMed() }
    .filterNotNull()
    .minsteEllerNull()

fun <T : Tidsenhet> Iterable<Tidslinje<*, T>>.tilOgMed() = this
    .map { it.tilOgMed() }
    .filterNotNull()
    .størsteEllerNull()

fun <I, T : Tidsenhet> Tidslinje<I, T>.tidsrom(): Collection<Tidspunkt<T>> = when {
    this.perioder().isEmpty() -> emptyList()
    else -> (perioder().first().fraOgMed.rangeTo(perioder().last().tilOgMed)).toList()
}

fun <T : Tidsenhet> Iterable<Tidslinje<*, T>>.tidsrom(): Collection<Tidspunkt<T>> = when {
    fraOgMed() == null || tilOgMed() == null -> emptyList()
    else -> (fraOgMed()!!..tilOgMed()!!).toList()
}

private fun <T : Tidsenhet> Iterable<Tidspunkt<T>>.størsteEllerNull() =
    this.reduceOrNull { acc, neste -> størsteAv(acc, neste) }

private fun <T : Tidsenhet> Iterable<Tidspunkt<T>>.minsteEllerNull() =
    this.reduceOrNull { acc, neste -> minsteAv(acc, neste) }

internal fun <T : Tidsenhet> størsteAv(t1: Tidspunkt<T>, t2: Tidspunkt<T>): Tidspunkt<T> =
    if (t1.erUendeligLengeTil() && t2.erEndelig() && t1.somEndelig() <= t2) {
        t2.neste().somUendeligLengeTil()
    } else if (t2.erUendeligLengeTil() && t1.erEndelig() && t2.somEndelig() <= t1) {
        t1.neste().somUendeligLengeTil()
    } else if (t1.erUendeligLengeTil() || t2.erUendeligLengeTil()) {
        maxOf(t1.somEndelig(), t2.somEndelig()).somUendeligLengeTil()
    } else {
        maxOf(t1, t2)
    }

internal fun <T : Tidsenhet> minsteAv(t1: Tidspunkt<T>, t2: Tidspunkt<T>): Tidspunkt<T> =
    if (t1.erUendeligLengeSiden() && t2.erEndelig() && t1.somEndelig() >= t2) {
        t2.forrige().somUendeligLengeSiden()
    } else if (t2.erUendeligLengeSiden() && t1.erEndelig() && t2.somEndelig() >= t1) {
        t1.forrige().somUendeligLengeSiden()
    } else if (t1.erUendeligLengeSiden() || t2.erUendeligLengeSiden()) {
        minOf(t1.somEndelig(), t2.somEndelig()).somUendeligLengeSiden()
    } else {
        minOf(t1, t2)
    }
