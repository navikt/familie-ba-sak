package no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt

abstract class SnittTidslinje<T>(
    avhengigheter: Collection<Tidslinje<*>>,
) : TidslinjeMedAvhengigheter<T>(avhengigheter) {

    var generertePerioder: Collection<Periode<T>>? = null

    constructor(vararg avhengighet: Tidslinje<*>) :
        this(avhengighet.asList())

    override fun perioder(): Collection<Periode<T>> {
        return generertePerioder ?: genererPerioder().also { generertePerioder = it }
    }

    private fun genererPerioder(): List<Periode<T>> =
        tidsrom().map { Pair(it, beregnSnitt(it)) }
            .fold(emptyList()) { acc, (tidspunkt, innhold) ->
                val sistePeriode = acc.lastOrNull()
                when {
                    sistePeriode != null && sistePeriode.innhold == innhold && sistePeriode.tom.erRettFør(tidspunkt.somEndelig()) ->
                        acc.replaceLast(sistePeriode.copy(tom = tidspunkt.somTilOgMed()))
                    else -> acc + Periode(tidspunkt.somFraOgMed(), tidspunkt.somTilOgMed(), innhold)
                }
            }

    protected abstract fun beregnSnitt(tidspunkt: Tidspunkt): T?

    companion object {
        private fun <T> Collection<T>.replaceLast(replacement: T) =
            this.take(this.size - 1) + replacement
    }
}

fun <T> Tidslinje<T>.hentUtsnitt(tidspunkt: Tidspunkt): T? =
    perioder().hentUtsnitt(tidspunkt)

fun <T> Collection<Periode<T>>.hentUtsnitt(tidspunkt: Tidspunkt): T? =
    this.filter { it.fom <= tidspunkt && it.tom >= tidspunkt }
        .firstOrNull()?.innhold
