package no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.fraOgMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.erRettFør
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.rangeTo
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tilOgMed

fun <T : Tidsenhet> Tidslinje<*, T>.tidsrom(): Iterable<Tidspunkt<T>> = fraOgMed()..tilOgMed()

fun <T : Tidsenhet> Iterable<Tidslinje<*, T>>.tidsrom(): Iterable<Tidspunkt<T>> = when {
    this.toList().isEmpty() -> emptyList()
    else -> fraOgMed()..tilOgMed()
}

fun <T : Tidsenhet, I> Iterable<Tidspunkt<T>>.tidslinjeFraTidspunkt(
    tidspunktMapper: (Tidspunkt<T>) -> Innholdsresultat<I>
): Tidslinje<I, T> = tidslinje {
    map { tidspunkt -> TidspunktMedInnholdsresultat(tidspunkt, tidspunktMapper(tidspunkt)) }
        .filter { it.harInnhold }
        .fold(emptyList()) { perioder, tidspunktMedInnholdsresultat ->
            val sistePeriode = perioder.lastOrNull()
            when {
                sistePeriode != null && sistePeriode.kanUtvidesMed(tidspunktMedInnholdsresultat) ->
                    perioder.replaceLast(sistePeriode.utvidMed(tidspunktMedInnholdsresultat))
                else -> perioder + tidspunktMedInnholdsresultat.tilPeriode()
            }
        }
}

/**
 * Innholdsresultat har tre tilstander
 * - uten innhold               -> harInnhold = false, harVerdi = false
 * - innhold som er null        -> harInnhold = true,  harVerdi = false
 * - innhold som ikke er null   -> harInnhold = true,  harVerdi = true
 */
data class Innholdsresultat<I>(
    val innhold: I?,
    internal val harInnhold: Boolean = true
) {
    constructor(innhold: I?) : this(innhold, true)

    companion object {
        fun <I> utenInnhold() = Innholdsresultat<I>(null, false)
    }

    val harVerdi
        get() = harInnhold && innhold != null

    val verdi
        get() = innhold!!

    fun <R> mapInnhold(mapper: (I?) -> R?): R? = if (this.harInnhold) mapper(verdi) else null
    fun <R> mapVerdi(mapper: (I) -> R): R? = if (this.harVerdi) mapper(verdi) else null
}

fun <I> I?.tilInnhold() = Innholdsresultat(this)
fun <I> I?.tilVerdi() = this?.let { Innholdsresultat<I>(it) } ?: Innholdsresultat.utenInnhold()

fun <I, T : Tidsenhet> Tidslinje<I, T>.innholdsresultatForTidspunkt(tidspunkt: Tidspunkt<T>): Innholdsresultat<I> =
    perioder().innholdsresultatForTidspunkt(tidspunkt)

fun <I, T : Tidsenhet> Collection<Periode<I, T>>.innholdsresultatForTidspunkt(
    tidspunkt: Tidspunkt<T>
): Innholdsresultat<I> {
    val periode = this.firstOrNull { it.omfatter(tidspunkt) }
    return when (periode) {
        null -> Innholdsresultat.utenInnhold()
        else -> Innholdsresultat(periode.innhold, true)
    }
}

private fun <I, T : Tidsenhet> Periode<I, T>.omfatter(tidspunkt: Tidspunkt<T>) =
    this.fraOgMed <= tidspunkt && this.tilOgMed >= tidspunkt

private data class TidspunktMedInnholdsresultat<I, T : Tidsenhet>(
    val tidspunkt: Tidspunkt<T>,
    val innholdsresultat: Innholdsresultat<I>
) {
    val harInnhold get() = innholdsresultat.harInnhold
    val innhold get() = innholdsresultat.innhold
}

private fun <I, T : Tidsenhet> Periode<I, T>.kanUtvidesMed(tidspunktMedInnholdsresultat: TidspunktMedInnholdsresultat<I, T>) =
    tidspunktMedInnholdsresultat.harInnhold &&
        this.innhold == tidspunktMedInnholdsresultat.innhold &&
        this.tilOgMed.erRettFør(tidspunktMedInnholdsresultat.tidspunkt.somEndelig())

private fun <I, T : Tidsenhet> Periode<I, T>.utvidMed(tidspunktMedInnholdsresultat: TidspunktMedInnholdsresultat<I, T>): Periode<I, T> =
    this.copy(tilOgMed = tidspunktMedInnholdsresultat.tidspunkt)

private fun <I, T : Tidsenhet> TidspunktMedInnholdsresultat<I, T>.tilPeriode() =
    Periode(this.tidspunkt.somFraOgMed(), this.tidspunkt.somTilOgMed(), this.innhold)
