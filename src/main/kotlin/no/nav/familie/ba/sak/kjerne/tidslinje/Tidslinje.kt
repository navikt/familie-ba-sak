package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TidslinjeMedAvhengigheter
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.TidspunktClosedRange
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.rangeTo

abstract class Tidslinje<I, T : Tidsenhet> {
    private var periodeCache: List<Periode<I, T>>? = null

    internal abstract fun fraOgMed(): Tidspunkt<T>
    internal abstract fun tilOgMed(): Tidspunkt<T>

    fun perioder(): Collection<Periode<I, T>> {
        return periodeCache ?: lagPerioder().sortedBy { it.fraOgMed }.toList()
            .also {
                valider(it)
                periodeCache = it
            }
    }

    protected abstract fun lagPerioder(): Collection<Periode<I, T>>

    protected open fun valider(perioder: List<Periode<I, T>>) {
        val perioderMedFeil = perioder.mapIndexed { index, periode ->
            when {
                index > 0 && periode.fraOgMed.erUendeligLengeSiden() ->
                    TidslinjeFeil(periode, this, TidslinjeFeilType.UENDELIG_FORTID_ETTER_FØRSTE_PERIODE)
                index < perioder.size - 1 && periode.tilOgMed.erUendeligLengeTil() ->
                    TidslinjeFeil(periode, this, TidslinjeFeilType.UENDELIG_FREMTID_FØR_SISTE_PERIODE)
                periode.fraOgMed > periode.tilOgMed ->
                    TidslinjeFeil(periode, this, TidslinjeFeilType.TOM_ER_FØR_FOM)
                index < index - 1 && perioder[index].tilOgMed > perioder[index + 1].fraOgMed ->
                    TidslinjeFeil(periode, this, TidslinjeFeilType.OVERLAPPER_ETTERFØLGENDE_PERIODE)
                else -> null
            }
        }.filterNotNull()

        perioderMedFeil.takeIf { it.isNotEmpty() }?.also {
            throw TidslinjeFeilException(it)
        }
    }

    override fun equals(other: Any?): Boolean {
        return if (other is Tidslinje<*, *>) {
            fraOgMed() == other.fraOgMed() &&
                tilOgMed() == other.tilOgMed() &&
                perioder() == other.perioder()
        } else
            false
    }

    override fun toString(): String =
        "[${fraOgMed()} - ${tilOgMed()}] " +
            lagPerioder().joinToString(" | ") { it.toString() }

    companion object {
        data class TidslinjeFeil(
            val periode: Periode<*, *>,
            val tidslinje: Tidslinje<*, *>,
            val type: TidslinjeFeilType
        )

        enum class TidslinjeFeilType {
            UENDELIG_FORTID_ETTER_FØRSTE_PERIODE,
            UENDELIG_FREMTID_FØR_SISTE_PERIODE,
            TOM_ER_FØR_FOM,
            OVERLAPPER_ETTERFØLGENDE_PERIODE,
        }

        class TidslinjeFeilException(tidslinjeFeil: Collection<TidslinjeFeil>) :
            IllegalStateException(tidslinjeFeil.toString())
    }
}

fun <I, T : Tidsenhet, R> Tidslinje<I, T>.map(mapper: (I?) -> R?): Tidslinje<R, T> {
    val avhengighet = this
    return object : TidslinjeMedAvhengigheter<R, T>(listOf(avhengighet)) {
        override fun lagPerioder() = avhengighet.perioder().map {
            Periode(it.fraOgMed, it.tilOgMed, mapper(it.innhold))
        }
    }
}

fun <T : Tidsenhet, I> TidspunktClosedRange<T>.tilTidslinje(innhold: () -> I): Tidslinje<I, T> {
    val fom = this.start
    val tom = this.endInclusive
    return object : Tidslinje<I, T>() {
        override fun fraOgMed() = fom
        override fun tilOgMed() = tom
        override fun lagPerioder(): Collection<Periode<I, T>> {
            return listOf(Periode(fom, tom, innhold()))
        }
    }
}

fun <T : Tidsenhet, I> Tidspunkt<T>.tilTidslinje(innhold: () -> I): Tidslinje<I, T> =
    this.rangeTo(this).tilTidslinje(innhold)

fun <I, T : Tidsenhet> Tidslinje<I, T>.filtrerMed(bolskTidslinje: Tidslinje<Boolean, T>): Tidslinje<I, T> {
    return this.kombinerMed(bolskTidslinje) { innhold, erSann ->
        when (erSann) {
            true -> innhold
            else -> null
        }
    }.beskjærEtter(this)
}

fun <I, T : Tidsenhet> Tidslinje<I, T>.beskjærEtter(beskjæring: Tidslinje<*, T>): Tidslinje<I, T> {

    val tidslinje = this
    return object : Tidslinje<I, T>() {
        override fun fraOgMed() = beskjæring.fraOgMed()
        override fun tilOgMed() = beskjæring.tilOgMed()

        override fun lagPerioder(): Collection<Periode<I, T>> {
            return tidslinje.perioder()
                .filter { it.fraOgMed <= tilOgMed() && it.tilOgMed >= fraOgMed() }
                .map {
                    when {
                        it.fraOgMed == tidslinje.fraOgMed() -> Periode(fraOgMed(), it.tilOgMed, it.innhold)
                        it.fraOgMed < fraOgMed() -> Periode(fraOgMed(), it.tilOgMed, it.innhold)
                        it.tilOgMed == tidslinje.tilOgMed() -> Periode(it.fraOgMed, tilOgMed(), it.innhold)
                        it.tilOgMed > tilOgMed() -> Periode(it.fraOgMed, tilOgMed(), it.innhold)
                        else -> it
                    }
                }
        }
    }
}
