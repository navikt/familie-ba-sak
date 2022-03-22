package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import java.time.temporal.Temporal

abstract class Tidslinje<T, TID : Temporal> {
    private var periodeCache: List<Periode<T, TID>>? = null

    internal abstract fun fraOgMed(): Tidspunkt<TID>
    internal abstract fun tilOgMed(): Tidspunkt<TID>

    fun perioder(): Collection<Periode<T, TID>> {
        return periodeCache ?: lagPerioder().sortedBy { it.fraOgMed }.toList()
            .also {
                valider(it)
                periodeCache = it
            }
    }

    protected abstract fun lagPerioder(): Collection<Periode<T, TID>>

    protected open fun valider(perioder: List<Periode<T, TID>>) {
        perioder.mapIndexed { index, periode ->
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
        }.filterNotNull().takeIf { it.isNotEmpty() }?.also { throw TidslinjeFeilException(it) }
    }

    override fun equals(other: Any?): Boolean {
        return if (other is Tidslinje<*, *>) {
            fraOgMed() == other.fraOgMed() &&
                tilOgMed() == other.tilOgMed() &&
                lagPerioder() == other.lagPerioder()
        } else
            false
    }

    override fun toString(): String =
        lagPerioder().map { it.toString() }.joinToString(" | ")

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

        class TidslinjeFeilException(tidslinjeFeil: Collection<TidslinjeFeil>) : IllegalStateException()
    }
}
