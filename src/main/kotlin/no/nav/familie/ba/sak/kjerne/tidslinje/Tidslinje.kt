package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TidslinjeMedAvhengigheter
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TidslinjeSomStykkerOppTiden
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.innholdForTidspunkt
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
        val perioderMedFeil = perioder.windowed(2) { (periode1, periode2) ->
            when {
                periode2.fraOgMed.erUendeligLengeSiden() ->
                    TidslinjeFeil(periode2, this, TidslinjeFeilType.UENDELIG_FORTID_ETTER_FØRSTE_PERIODE)
                periode1.tilOgMed.erUendeligLengeTil() ->
                    TidslinjeFeil(periode1, this, TidslinjeFeilType.UENDELIG_FREMTID_FØR_SISTE_PERIODE)
                periode1.fraOgMed > periode1.tilOgMed ->
                    TidslinjeFeil(periode1, this, TidslinjeFeilType.TOM_ER_FØR_FOM)
                periode1.tilOgMed >= periode2.fraOgMed ->
                    TidslinjeFeil(periode1, this, TidslinjeFeilType.OVERLAPPER_ETTERFØLGENDE_PERIODE)
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

fun <V, H, R, T : Tidsenhet> Tidslinje<V, T>.snittKombinerMed(
    høyre: Tidslinje<H, T>,
    kombinator: (V?, H?) -> R?
): Tidslinje<R, T> {
    val venstre = this
    return object : TidslinjeSomStykkerOppTiden<R, T>(venstre, høyre) {
        override fun finnInnholdForTidspunkt(tidspunkt: Tidspunkt<T>): R? =
            kombinator(venstre.innholdForTidspunkt(tidspunkt), høyre.innholdForTidspunkt(tidspunkt))
    }
}
