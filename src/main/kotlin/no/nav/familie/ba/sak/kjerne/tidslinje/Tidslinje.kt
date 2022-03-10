package no.nav.familie.ba.sak.kjerne.eøs.temaperiode

import java.time.YearMonth

abstract class Tidslinje<T> {
    internal abstract val fraOgMed: Tidspunkt
    internal abstract val tilOgMed: Tidspunkt
    abstract val perioder: Collection<Periode<T>>

    // Sammenhengende perioder (ingen hull bruk NULL-perioder)
    // Ingen overlappende perioder
    open fun valider(): Collection<TidslinjeFeil> {
        val sorterte = perioder.sortedBy { it.fom }.toList()

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

    fun hentUtsnitt(tidspunkt: Tidspunkt): PeriodeUtsnitt<T> {
        return perioder.filter { it.fom <= tidspunkt && it.tom >= tidspunkt }
            .firstOrNull()?.let { PeriodeUtsnitt(it.innhold, it.id) } ?: throw Error()
    }
}

abstract class AvhengigTidslinje<T>(
    private val avhengigheter: Collection<Tidslinje<*>>
) : Tidslinje<T>() {

    override val fraOgMed: Tidspunkt
        get() = avhengigheter.minOf { it.fraOgMed }
    override val tilOgMed: Tidspunkt
        get() = avhengigheter.maxOf { it.tilOgMed }

    // Har samme bredde som foregående æraer
    // Refererer alle perioder på foregående æra(er)
    // Ingen løse referanser til perioder på foregående æraer
    override fun valider(): List<TidslinjeFeil> {
        val konsistensFeil = super.valider()
        val foregåendeFeil = avhengigheter.map { it.valider() }.flatten()

        val foregåendePerioder = avhengigheter.flatMap { it.perioder }.toList()
        val foregåendePeriodeIder = foregåendePerioder.map { it.id }
        val referertePeriodeIder = perioder.flatMap { it.avhengerAv }.toSet()

        val manglendeReferanserFeil = foregåendePeriodeIder.minus(referertePeriodeIder).map { id ->
            TidslinjeFeil(
                foregåendePerioder.first { it.id == id },
                this,
                TidslinjeFeilType.MANGLER_REFERANSE_TIL_PERIODE
            )
        }

        val overflødigeReferanserFeil = referertePeriodeIder.minus(foregåendePeriodeIder)
            .flatMap { id -> perioder.filter { it.avhengerAv.contains(id) } }
            .map { TidslinjeFeil(it, this, TidslinjeFeilType.REFERER_TIL_UTGÅTT_PERIODE) }

        // TODO: Må sjekke at vi referer til avhengigheter i samme rekkefølge som de faktisk forekommer

        return foregåendeFeil + konsistensFeil + manglendeReferanserFeil + overflødigeReferanserFeil
    }
}

abstract class KalkulerendeTidslinje<T>(
    avhengigheter: Collection<Tidslinje<*>>,
    serialiserer: TidslinjeSerialiserer<T> = IngenTidslinjeSerialisering()
) : AvhengigTidslinje<T>(avhengigheter) {

    constructor(avhengighet: Tidslinje<*>, serialiserer: TidslinjeSerialiserer<T> = IngenTidslinjeSerialisering()) :
        this(listOf(avhengighet), serialiserer)

    protected abstract fun kalkulerInnhold(tidspunkt: Tidspunkt): PeriodeInnhold<T>

    override val perioder: Collection<Periode<T>> by lazy {
        serialiserer.hentEllerOpprett { genererPerioder() }
    }

    private fun genererPerioder(): List<Periode<T>> =
        (fraOgMed..tilOgMed).map { Pair(it, kalkulerInnhold(it)) }
            .fold(emptyList()) { acc, (måned, innhold) ->
                val sistePeriode = acc.lastOrNull()
                when {
                    sistePeriode != null && sistePeriode.tilInnhold() == innhold && sistePeriode.fom.erRettFør(måned) ->
                        acc.replaceLast(sistePeriode.copy(tom = måned))
                    else -> acc + innhold.tilPeriode(måned)
                }
            }
}

class SelvbyggerTema<T>(
    egnePerioder: Collection<Periode<T>>
) : Tidslinje<T>() {
    override val fraOgMed = egnePerioder.minOf { it.fom }
    override val tilOgMed = egnePerioder.maxOf { it.tom }
    override val perioder = egnePerioder
}

private fun <T> Collection<T>.replaceLast(replacement: T) =
    this.take(this.size - 1) + replacement

private fun YearMonth?.erRettFør(måned: YearMonth) =
    this?.plusMonths(1) == måned

fun Collection<PeriodeUtsnitt<*>>.idListe() = this.map { it.id }

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
    MANGLER_REFERANSE_TIL_PERIODE,
    REFERANSER_I_FEIL_REKKEFØLGE,
    REFERER_TIL_UTGÅTT_PERIODE,
}

interface TidslinjeSerialiserer<T> {
    fun lagre(perioder: Collection<Periode<T>>): Collection<Periode<T>>
    fun hent(): Collection<Periode<T>>
}

class IngenTidslinjeSerialisering<T> : TidslinjeSerialiserer<T> {
    override fun lagre(perioder: Collection<Periode<T>>): Collection<Periode<T>> {
        return perioder
    }

    override fun hent(): Collection<Periode<T>> = emptyList()
}

fun <T> TidslinjeSerialiserer<T>.hentEllerOpprett(generator: () -> Collection<Periode<T>>): Collection<Periode<T>> {
    val hentet = this.hent()
    if (hentet.isEmpty())
        return lagre(generator())
    else
        return hentet
}
