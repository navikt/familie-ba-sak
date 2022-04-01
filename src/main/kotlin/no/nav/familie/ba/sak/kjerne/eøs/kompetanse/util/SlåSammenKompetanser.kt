package no.nav.familie.ba.sak.kjerne.eøs.kompetanse.util

import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TidslinjeSomStykkerOppTiden
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.innholdForTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt.Companion.tilTidspunktEllerUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt.Companion.tilTidspunktEllerUendeligLengeTil
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import java.time.YearMonth

fun Collection<Kompetanse>.slåSammen(): Collection<Kompetanse> {
    return if (this.isEmpty()) {
        this
    } else {
        SlåSammenKompetanserTidslinje(this.map { EnkeltKompetanseTidslinje(it) })
            .perioder().flatMap { periode -> periode.innhold?.settFomOgTom(periode) ?: emptyList() }
    }
}

fun Iterable<Kompetanse>?.settFomOgTom(periode: Periode<*, Måned>) =
    this?.map { kompetanse -> kompetanse.settFomOgTom(periode) }

fun Kompetanse.settFomOgTom(periode: Periode<*, Måned>) =
    this.copy(
        fom = periode.fraOgMed.tilYearMonthEllerNull(),
        tom = periode.tilOgMed.tilYearMonthEllerNull()
    )

internal class EnkeltKompetanseTidslinje(
    val kompetanse: Kompetanse
) : Tidslinje<Kompetanse, Måned>() {
    override fun fraOgMed() = kompetanse.fom.tilTidspunktEllerUendeligLengeSiden { kompetanse.tom ?: YearMonth.now() }

    override fun tilOgMed(): MånedTidspunkt {
        return when {
            kompetanse.tom != null && kompetanse.tom.isAfter(inneværendeMåned()) -> Tidspunkt.uendeligLengeTil(
                kompetanse.tom
            )
            else -> kompetanse.tom.tilTidspunktEllerUendeligLengeTil { kompetanse.tom ?: YearMonth.now() }
        }
    }

    override fun lagPerioder(): Collection<Periode<Kompetanse, Måned>> {
        return listOf(Periode(fraOgMed(), tilOgMed(), kompetanse.copy(fom = null, tom = null)))
    }
}

internal class SlåSammenKompetanserTidslinje(
    val kompetanseTidslinjer: Collection<EnkeltKompetanseTidslinje>
) : TidslinjeSomStykkerOppTiden<Set<Kompetanse>, Måned>(kompetanseTidslinjer) {
    override fun finnInnholdForTidspunkt(tidspunkt: Tidspunkt<Måned>): Set<Kompetanse> {
        return kompetanseTidslinjer
            .mapNotNull { it.innholdForTidspunkt(tidspunkt) }
            .fold(mutableSetOf()) { kompetanser, kompetanse ->
                val matchendeKompetanse = kompetanser.plukkUtHvis { it.erLikUtenBarn(kompetanse) }
                val oppdatertKompetanse = matchendeKompetanse?.leggSammenBarn(kompetanse) ?: kompetanse
                kompetanser.add(oppdatertKompetanse)
                kompetanser
            }
    }

    private fun <T> MutableSet<T>.plukkUtHvis(predicate: (T) -> Boolean): T? =
        this.find { predicate(it) }?.also { this.remove(it) }

    private fun Kompetanse.erLikUtenBarn(kompetanse: Kompetanse) =
        this.copy(barnAktører = emptySet()) == kompetanse.copy(barnAktører = emptySet())

    private fun Kompetanse.leggSammenBarn(kompetanse: Kompetanse) =
        this.copy(barnAktører = this.barnAktører + kompetanse.barnAktører)
}
