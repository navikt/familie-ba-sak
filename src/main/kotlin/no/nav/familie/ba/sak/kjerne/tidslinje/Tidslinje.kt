package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.familie.ba.sak.kjerne.tidslinje.tid.NullTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet

abstract class Tidslinje<I, T : Tidsenhet> {
    private var periodeCache: List<Periode<I, T>>? = null

    fun perioder(): Collection<Periode<I, T>> {
        return periodeCache ?: lagPerioder().sortedBy { it.fraOgMed }.toList()
            .also {
                valider(it)
                periodeCache = it
            }
    }

    protected abstract fun lagPerioder(): Collection<Periode<I, T>>

    protected open fun valider(perioder: List<Periode<I, T>>) {

        val feilInnenforPerioder = perioder.map {
            when {
                it.fraOgMed > it.tilOgMed ->
                    TidslinjeFeil(it, this, TidslinjeFeilType.TOM_ER_FØR_FOM)
                else -> null
            }
        }

        val feilMellomPåfølgendePerioder = perioder.windowed(2) { (periode1, periode2) ->
            when {
                periode2.fraOgMed.erUendeligLengeSiden() ->
                    TidslinjeFeil(periode2, this, TidslinjeFeilType.UENDELIG_FORTID_ETTER_FØRSTE_PERIODE)
                periode1.tilOgMed.erUendeligLengeTil() ->
                    TidslinjeFeil(periode1, this, TidslinjeFeilType.UENDELIG_FREMTID_FØR_SISTE_PERIODE)
                periode1.tilOgMed >= periode2.fraOgMed ->
                    TidslinjeFeil(periode1, this, TidslinjeFeilType.OVERLAPPER_ETTERFØLGENDE_PERIODE)
                else -> null
            }
        }

        val tidslinjeFeil = (feilInnenforPerioder + feilMellomPåfølgendePerioder)
            .filterNotNull()

        if (tidslinjeFeil.isNotEmpty()) {
            throw TidslinjeFeilException(tidslinjeFeil)
        }
    }

    override fun equals(other: Any?): Boolean {
        return if (other is Tidslinje<*, *>) {
            perioder() == other.perioder()
        } else
            false
    }

    override fun toString(): String =
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

        data class TidslinjeFeilException(val tidslinjeFeil: Collection<TidslinjeFeil>) :
            IllegalStateException()
    }
}

fun <I, T : Tidsenhet> Tidslinje<I, T>.fraOgMed() =
    this.perioder().firstOrNull()?.fraOgMed ?: NullTidspunkt.fraOgMed()

fun <I, T : Tidsenhet> Tidslinje<I, T>.tilOgMed() =
    this.perioder().lastOrNull()?.tilOgMed ?: NullTidspunkt.tilOgMed()

fun <I, T : Tidsenhet> tidslinje(lagPerioder: () -> Collection<Periode<I, T>>) =
    object : Tidslinje<I, T>() {
        override fun lagPerioder() = lagPerioder()
    }
