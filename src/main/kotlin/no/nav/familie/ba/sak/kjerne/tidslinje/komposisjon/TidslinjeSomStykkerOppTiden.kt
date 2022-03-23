package no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.rangeTo

abstract class TidslinjeSomStykkerOppTiden<I, T : Tidsenhet>(
    avhengigheter: Collection<Tidslinje<*, T>>,
) : TidslinjeMedAvhengigheter<I, T>(avhengigheter) {
    constructor(vararg avhengighet: Tidslinje<*, T>) :
        this(avhengighet.asList())

    override fun lagPerioder(): Collection<Periode<I, T>> {
        val tidspunkter = fraOgMed()..tilOgMed()
        return tidspunkter.map { tidspunkt -> TidspunktMedInnhold(tidspunkt, finnInnholdForTidspunkt(tidspunkt)) }
            .fold(emptyList()) { perioder, tidspunktMedInnhold ->
                val sistePeriode = perioder.lastOrNull()
                when {
                    sistePeriode != null && sistePeriode.kanUtvidesMed(tidspunktMedInnhold) ->
                        perioder.replaceLast(sistePeriode.utvidMed(tidspunktMedInnhold))
                    else -> perioder + tidspunktMedInnhold.tilPeriode()
                }
            }
    }

    protected abstract fun finnInnholdForTidspunkt(tidspunkt: Tidspunkt<T>): I?

    companion object {
        private fun <T> Collection<T>.replaceLast(replacement: T) =
            this.take(this.size - 1) + replacement
    }
}

fun <I, T : Tidsenhet> Tidslinje<I, T>.hentUtsnitt(tidspunkt: Tidspunkt<T>): I? =
    perioder().hentUtsnitt(tidspunkt)

fun <I, T : Tidsenhet> Collection<Periode<I, T>>.hentUtsnitt(tidspunkt: Tidspunkt<T>): I? =
    this.filter { it.fraOgMed <= tidspunkt && it.tilOgMed >= tidspunkt }
        .firstOrNull()?.innhold

private fun <I, T : Tidsenhet> Periode<I, T>.kanUtvidesMed(tidspunktMedInnhold: TidspunktMedInnhold<I, T>) =
    this.innhold == tidspunktMedInnhold.innhold &&
        this.tilOgMed.erRettFør(tidspunktMedInnhold.tidspunkt.somEndelig())

private fun <I, T : Tidsenhet> Periode<I, T>.utvidMed(tidspunktMedInnhold: TidspunktMedInnhold<I, T>): Periode<I, T> =
    this.copy(tilOgMed = tidspunktMedInnhold.tidspunkt)

private data class TidspunktMedInnhold<I, T : Tidsenhet>(
    val tidspunkt: Tidspunkt<T>,
    val innhold: I?
)

private fun <I, T : Tidsenhet> TidspunktMedInnhold<I, T>.tilPeriode() =
    Periode(this.tidspunkt.somFraOgMed(), this.tidspunkt.somTilOgMed(), this.innhold)
