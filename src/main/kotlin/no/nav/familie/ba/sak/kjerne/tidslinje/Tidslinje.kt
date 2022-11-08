package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.erUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.erUendeligLengeTil
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.minsteEllerNull
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.rangeTo
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.størsteEllerNull

/**
 * Base-klassen for alle tidslinjer. Bygger på en tanke om at en tidslinje inneholder en
 * sortert liste av ikke-overlappende perioder med et innhold av type I, som kan være null.
 * Tidslinjen og tilhørende perioder har alle tidsenheten T.
 * Periodene er sortert fra tidligste til seneste.
 * fraOgMed og tilOgMed i en periode kan være like, men tilOgMed kan aldri være tidligere enn fraOgMed
 * fraOgMed i første periode kan være åpen, dvs "uenedelig lenge siden"
 * tilOgMed i siste periode kan være åpen, dvs "uendelig lenge til"
 * Generelt vil to påfølgende perioder kunne slås sammen hvis de ligger inntil hverandre
 * og innholdet er likt. Likhet avgjøres av [equals()]
 */
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
                    TidslinjeFeil(periode = it, tidslinje = this, type = TidslinjeFeilType.TOM_ER_FØR_FOM)
                else -> null
            }
        }

        val feilMellomPåfølgendePerioder = perioder.windowed(2) { (periode1, periode2) ->
            when {
                periode2.fraOgMed.erUendeligLengeSiden() ->
                    TidslinjeFeil(
                        periode = periode2,
                        tidslinje = this,
                        type = TidslinjeFeilType.UENDELIG_FORTID_ETTER_FØRSTE_PERIODE
                    )
                periode1.tilOgMed.erUendeligLengeTil() ->
                    TidslinjeFeil(
                        periode = periode1,
                        tidslinje = this,
                        type = TidslinjeFeilType.UENDELIG_FREMTID_FØR_SISTE_PERIODE
                    )
                periode1.tilOgMed >= periode2.fraOgMed ->
                    TidslinjeFeil(
                        periode = periode1,
                        tidslinje = this,
                        type = TidslinjeFeilType.OVERLAPPER_ETTERFØLGENDE_PERIODE
                    )
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
        } else {
            false
        }
    }

    override fun toString(): String =
        lagPerioder().joinToString(" | ") { it.toString() }

    companion object {
        data class TidslinjeFeil(
            val type: TidslinjeFeilType,
            val periode: Periode<*, *>,
            val tidslinje: Tidslinje<*, *>
        )

        enum class TidslinjeFeilType {
            UENDELIG_FORTID_ETTER_FØRSTE_PERIODE,
            UENDELIG_FREMTID_FØR_SISTE_PERIODE,
            TOM_ER_FØR_FOM,
            OVERLAPPER_ETTERFØLGENDE_PERIODE
        }

        data class TidslinjeFeilException(val tidslinjeFeil: Collection<TidslinjeFeil>) :
            IllegalStateException(tidslinjeFeil.toString())
    }
}

fun <I, T : Tidsenhet> Tidslinje<I, T>.fraOgMed() =
    this.perioder().firstOrNull()?.fraOgMed

fun <I, T : Tidsenhet> Tidslinje<I, T>.tilOgMed() =
    this.perioder().lastOrNull()?.tilOgMed

fun <T : Tidsenhet> Iterable<Tidslinje<*, T>>.fraOgMed() = this
    .map { it.fraOgMed() }
    .filterNotNull()
    .minsteEllerNull()

fun <T : Tidsenhet> Iterable<Tidslinje<*, T>>.tilOgMed() = this
    .map { it.tilOgMed() }
    .filterNotNull()
    .størsteEllerNull()

fun <I, T : Tidsenhet> Tidslinje<I, T>.tidsrom(): Collection<Tidspunkt<T>> = when {
    this.perioder().isEmpty() -> emptyList()
    else -> (perioder().first().fraOgMed.rangeTo(perioder().last().tilOgMed)).toList()
}

fun <T : Tidsenhet> Iterable<Tidslinje<*, T>>.tidsrom(): Collection<Tidspunkt<T>> = when {
    fraOgMed() == null || tilOgMed() == null -> emptyList()
    else -> (fraOgMed()!!..tilOgMed()!!).toList()
}

fun <I, T : Tidsenhet> tidslinje(lagPerioder: () -> Collection<Periode<I, T>>) =
    object : Tidslinje<I, T>() {
        override fun lagPerioder() = lagPerioder()
    }
