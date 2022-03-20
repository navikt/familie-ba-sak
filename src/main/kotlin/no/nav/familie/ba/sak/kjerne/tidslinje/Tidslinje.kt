package no.nav.familie.ba.sak.kjerne.tidslinje

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
}

abstract class TidslinjeMedAvhengigheter<T>(
    private val foregåendeTidslinjer: Collection<Tidslinje<*>>
) : Tidslinje<T>() {

    override fun tidsrom(): Tidsrom {
        val fom = foregåendeTidslinjer.map { it.tidsrom().start }.minsteEllerUendelig()
        val tom = foregåendeTidslinjer.map { it.tidsrom().endInclusive }.størsteEllerUendelig()

        return fom..tom
    }
}

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
        .firstOrNull()?.let { it.innhold }

class KomprimerendeTidslinje<T>(val tidslinje: Tidslinje<T>) : SnittTidslinje<T>(tidslinje) {
    override fun beregnSnitt(tidspunkt: Tidspunkt): T? = tidslinje.hentUtsnitt(tidspunkt)
}

fun <T> Tidslinje<T>.komprimer(): Tidslinje<T> = KomprimerendeTidslinje(this)

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
