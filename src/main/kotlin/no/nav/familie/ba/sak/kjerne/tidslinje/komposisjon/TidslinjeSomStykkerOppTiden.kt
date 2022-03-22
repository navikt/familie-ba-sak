package no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.rangeTo
import java.time.temporal.Temporal

abstract class TidslinjeSomStykkerOppTiden<T, TID : Temporal>(
    avhengigheter: Collection<Tidslinje<*, TID>>,
) : TidslinjeMedAvhengigheter<T, TID>(avhengigheter) {
    constructor(vararg avhengighet: Tidslinje<*, TID>) :
        this(avhengighet.asList())

    override fun lagPerioder(): Collection<Periode<T, TID>> {
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

    protected abstract fun finnInnholdForTidspunkt(tidspunkt: Tidspunkt<TID>): T?

    companion object {
        private fun <T> Collection<T>.replaceLast(replacement: T) =
            this.take(this.size - 1) + replacement
    }
}

fun <T, TID : Temporal> Tidslinje<T, TID>.hentUtsnitt(tidspunkt: Tidspunkt<TID>): T? =
    perioder().hentUtsnitt(tidspunkt)

fun <T, TID : Temporal> Collection<Periode<T, TID>>.hentUtsnitt(tidspunkt: Tidspunkt<TID>): T? =
    this.filter { it.fraOgMed <= tidspunkt && it.tilOgMed >= tidspunkt }
        .firstOrNull()?.innhold

private fun <T, TID : Temporal> Periode<T, TID>.kanUtvidesMed(tidspunktMedInnhold: TidspunktMedInnhold<T, TID>) =
    this.innhold == tidspunktMedInnhold.innhold &&
        this.tilOgMed.erRettFør(tidspunktMedInnhold.tidspunkt.somEndelig())

private fun <T, TID : Temporal> Periode<T, TID>.utvidMed(tidspunktMedInnhold: TidspunktMedInnhold<T, TID>): Periode<T, TID> =
    this.copy(tilOgMed = tidspunktMedInnhold.tidspunkt)

private data class TidspunktMedInnhold<T, TID : Temporal>(
    val tidspunkt: Tidspunkt<TID>,
    val innhold: T?
)

private fun <T, TID : Temporal> TidspunktMedInnhold<T, TID>.tilPeriode() =
    Periode(this.tidspunkt.somFraOgMed(), this.tidspunkt.somTilOgMed(), this.innhold)
