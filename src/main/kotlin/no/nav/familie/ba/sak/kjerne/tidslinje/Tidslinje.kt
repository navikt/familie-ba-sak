package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt

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
