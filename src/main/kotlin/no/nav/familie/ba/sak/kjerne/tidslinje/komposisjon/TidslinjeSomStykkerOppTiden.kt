package no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.rangeTo

abstract class TidslinjeSomStykkerOppTiden<T>(
    avhengigheter: Collection<Tidslinje<*>>,
) : TidslinjeMedAvhengigheter<T>(avhengigheter) {
    constructor(vararg avhengighet: Tidslinje<*>) :
        this(avhengighet.asList())

    override fun lagPerioder(): Collection<Periode<T>> {
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

    protected abstract fun finnInnholdForTidspunkt(tidspunkt: Tidspunkt): T?

    companion object {
        private fun <T> Collection<T>.replaceLast(replacement: T) =
            this.take(this.size - 1) + replacement
    }
}

fun <T> Tidslinje<T>.hentUtsnitt(tidspunkt: Tidspunkt): T? =
    perioder().hentUtsnitt(tidspunkt)

fun <T> Collection<Periode<T>>.hentUtsnitt(tidspunkt: Tidspunkt): T? =
    this.firstOrNull { it.fraOgMed <= tidspunkt && it.tilOgMed >= tidspunkt }?.innhold

private fun <T> Periode<T>.kanUtvidesMed(tidspunktMedInnhold: TidspunktMedInnhold<T>) =
    this.innhold == tidspunktMedInnhold.innhold &&
        this.tilOgMed.erRettFør(tidspunktMedInnhold.tidspunkt.somEndelig())

private fun <T> Periode<T>.utvidMed(tidspunktMedInnhold: TidspunktMedInnhold<T>): Periode<T> =
    this.copy(tilOgMed = tidspunktMedInnhold.tidspunkt)

private data class TidspunktMedInnhold<T>(
    val tidspunkt: Tidspunkt,
    val innhold: T?
)

private fun <T> TidspunktMedInnhold<T>.tilPeriode() =
    Periode(this.tidspunkt.somFraOgMed(), this.tidspunkt.somTilOgMed(), this.innhold)
