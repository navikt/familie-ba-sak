package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsrom

abstract class Tidslinje<T> {
    internal abstract fun tidsrom(): Tidsrom
    abstract fun perioder(): Collection<Periode<T>>

    // Sammenhengende perioder (ingen hull bruk NULL-perioder)
    // Ingen overlappende perioder
    open fun valider(): Collection<TidslinjeFeil> {
        val sorterte = perioder().sortedBy { it.fom }.toList()

        return sorterte.mapIndexed { index, periode ->
            when {
                index > 0 && periode.fom.erUendeligLengeSiden() ->
                    TidslinjeFeil(periode, this, TidslinjeFeilType.UENDELIG_FORTID_ETTER_FØRSTE_PERIODE)
                index < sorterte.size - 1 && periode.tom.erUendeligLengeTil() ->
                    TidslinjeFeil(periode, this, TidslinjeFeilType.`UE#NDELIG_FREMTID_FØR_SISTE_PERIODE`)
                index >= 0 && index < sorterte.size - 1 && !sorterte[index].tom.erRettFør(sorterte[index + 1].fom) ->
                    TidslinjeFeil(periode, this, TidslinjeFeilType.PERIODE_BLIR_IKKE_FULGT_AV_PERIODE)
                periode.fom > periode.tom ->
                    TidslinjeFeil(periode, this, TidslinjeFeilType.TOM_ER_FØR_FOM)
                else -> null
            }
        }.filterNotNull()
    }

    override fun equals(other: Any?): Boolean {
        return if (other is Tidslinje<*>) {
            tidsrom().start == other.tidsrom().start &&
                tidsrom().endInclusive == other.tidsrom().endInclusive &&
                perioder() == other.perioder()
        } else
            false
    }

    override fun toString(): String =
        perioder().map { it.toString() }.joinToString(" | ")

    companion object {
        data class TidslinjeFeil(
            val periode: Periode<*>,
            val tidslinje: Tidslinje<*>,
            val type: TidslinjeFeilType
        )

        enum class TidslinjeFeilType {
            UENDELIG_FORTID_ETTER_FØRSTE_PERIODE,
            `UE#NDELIG_FREMTID_FØR_SISTE_PERIODE`,
            PERIODE_BLIR_IKKE_FULGT_AV_PERIODE,
            TOM_ER_FØR_FOM,
        }
    }
}
