package no.nav.familie.ba.sak.kjerne.eøs.temaperiode

import java.time.YearMonth

abstract class Tidslinje<T> {
    private var etterfølgendeTidslinjer: MutableList<TidslinjeMedAvhengigheter<*>> = mutableListOf()
    private var gjeldendePerioder: Collection<Periode<T>>? = null

    internal abstract val tidsrom: Tidsrom

    val perioder: Collection<Periode<T>>
        get() =
            gjeldendePerioder ?: genererPerioder(tidsrom).also { gjeldendePerioder = it }

    internal fun registrerEtterfølgendeTidslinje(tidslinje: TidslinjeMedAvhengigheter<*>) {
        etterfølgendeTidslinjer.add(tidslinje)
    }

    protected fun oppdaterPerioder(perioder: Collection<Periode<T>>) {
        gjeldendePerioder = perioder
    }

    protected abstract fun genererPerioder(tidsrom: Tidsrom): Collection<Periode<T>>

    internal fun splitt(splitt: PeriodeSplitt<*>) {
        perioder
            .flatMap { splitt.påførSplitt(it, etterfølgendeTidslinjer) }
            .also { oppdaterPerioder(it) }
    }

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

abstract class TidslinjeUtenAvhengigheter<T> : Tidslinje<T>() {
    fun splitt(tidspunkt: Tidspunkt) {
        splitt(PeriodeSplitt<T>(tidspunkt))
    }
}

abstract class TidslinjeMedEksterntInnhold<T>(
    protected val innhold: Iterable<T>,
) : TidslinjeUtenAvhengigheter<T>()

abstract class TidslinjeMedAvhengigheter<T>(
    private val foregåendeTidslinjer: Collection<Tidslinje<*>>
) : Tidslinje<T>() {

    private val fom = foregåendeTidslinjer.minOf { it.tidsrom.start }
    private val tom = foregåendeTidslinjer.maxOf { it.tidsrom.endInclusive }
    override val tidsrom = fom..tom

    init {
        foregåendeTidslinjer.forEach { it.registrerEtterfølgendeTidslinje(this) }
    }

    // Refererer alle perioder på foregående tidslinje(r)
    // Ingen løse referanser til perioder på foregående æraer
    override fun valider(): List<TidslinjeFeil> {
        val konsistensFeil = super.valider()

        // Tror ikke det er smart å gjøre rekursiv validering
        val foregåendeFeil = foregåendeTidslinjer.map { it.valider() }.flatten()

        val foregåendePerioder = foregåendeTidslinjer.flatMap { it.perioder }.toList()
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

        // TODO: Må sjekke at vi refererer til avhengigheter i samme rekkefølge som de faktisk forekommer

        return foregåendeFeil + konsistensFeil + manglendeReferanserFeil + overflødigeReferanserFeil
    }
}

abstract class KalkulerendeTidslinje<T>(
    avhengigheter: Collection<Tidslinje<*>>,
) : TidslinjeMedAvhengigheter<T>(avhengigheter) {

    constructor(vararg avhengighet: Tidslinje<*>) :
        this(avhengighet.asList())

    override fun genererPerioder(tidsrom: Tidsrom): List<Periode<T>> =
        tidsrom.map { Pair(it, kalkulerInnhold(it)) }
            .fold(emptyList()) { acc, (måned, innhold) ->
                val sistePeriode = acc.lastOrNull()
                when {
                    sistePeriode != null && sistePeriode.tilInnhold() == innhold && sistePeriode.fom.erRettFør(måned) ->
                        acc.replaceLast(sistePeriode.copy(tom = måned))
                    else -> acc + innhold.tilPeriode(måned)
                }
            }

    protected abstract fun kalkulerInnhold(tidspunkt: Tidspunkt): PeriodeInnhold<T>
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

fun <T> Collection<Periode<T>>.finnHull(fraOgMed: Tidspunkt, tilOgMed: Tidspunkt): Collection<Tidsrom> {

    if (this.isEmpty())
        return listOf(Tidsrom(fraOgMed, tilOgMed))
    else {
        val sortert = this.sortedBy { it.fom }.toList()

        val tidligste = fraOgMed..sortert.first().fom.forrige()
        val seneste = sortert.last().tom.neste()..tilOgMed
        val inni = sortert.mapIndexed { index, periode ->
            when (index) {
                0 -> Tidsrom.NULL
                sortert.size - 1 -> Tidsrom.NULL
                else -> sortert[index - 1].tom.neste()..periode.fom.forrige()
            }
        }

        val alle = listOf(tidligste) + inni + listOf(seneste)
        return alle.filter { !it.isEmpty() }
    }
}
