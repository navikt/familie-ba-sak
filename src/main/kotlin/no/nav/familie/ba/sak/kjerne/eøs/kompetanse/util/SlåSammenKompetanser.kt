package no.nav.familie.ba.sak.kjerne.eøs.kompetanse.util

import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.MIN_MÅNED
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.utenBarn
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.utenPeriode
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombiner
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.komprimer
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt.Companion.tilTidspunktEllerUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt.Companion.tilTidspunktEllerUendeligLengeTil
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.minsteEllerNull
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.størsteEllerNull
import java.time.YearMonth

fun Collection<Kompetanse>.slåSammen(): Collection<Kompetanse> {

    if (this.isEmpty())
        return this

    val kompetanseSettTidslinje: Tidslinje<Set<Kompetanse>, Måned> = this.map { KompetanseTidslinje(it) }
        .kombiner {
            it.groupingBy { it.utenBarn() }.reduce { _, acc, kompetanse -> acc.leggSammenBarn(kompetanse) }
                .values.toSet()
        }

    val kompetanserSlåttSammenVertikalt = kompetanseSettTidslinje.perioder().flatMap { periode ->
        periode.innhold?.settFomOgTom(periode) ?: emptyList()
    }

    val kompetanseSlåttSammenHorisontalt = kompetanserSlåttSammenVertikalt
        .groupBy { it.utenPeriode() }
        .mapValues { (_, kompetanser) -> KompetanseTidslinje(kompetanser).komprimer() }
        .mapValues { (_, tidslinje) -> tidslinje.perioder() }
        .values.flatten().mapNotNull { periode -> periode.innhold?.settFomOgTom(periode) }

    return kompetanseSlåttSammenHorisontalt
}

internal class KompetanseTidslinje(
    val kompetanser: List<Kompetanse>
) : Tidslinje<Kompetanse, Måned>() {

    constructor(vararg kompetanser: Kompetanse) : this(kompetanser.toList())

    override fun fraOgMed() =
        kompetanser.map { it.fraOgMedTidspunkt() }.minsteEllerNull() ?: throw IllegalArgumentException()

    override fun tilOgMed() =
        kompetanser.map { it.tilOgMedTidspunkt() }.størsteEllerNull() ?: throw IllegalArgumentException()

    override fun lagPerioder(): Collection<Periode<Kompetanse, Måned>> =
        kompetanser.sortedBy { it.fom ?: MIN_MÅNED }
            .map { Periode(it.fraOgMedTidspunkt(), it.tilOgMedTidspunkt(), it.utenPeriode()) }
}

private fun Kompetanse.leggSammenBarn(kompetanse: Kompetanse) =
    this.copy(barnAktører = this.barnAktører + kompetanse.barnAktører)

private fun Kompetanse.fraOgMedTidspunkt(): Tidspunkt<Måned> =
    this.fom.tilTidspunktEllerUendeligLengeSiden { this.tom ?: YearMonth.now() }

private fun Kompetanse.tilOgMedTidspunkt(): Tidspunkt<Måned> = when {
    this.tom != null && this.tom.isAfter(inneværendeMåned()) -> Tidspunkt.uendeligLengeTil(this.tom)
    else -> this.tom.tilTidspunktEllerUendeligLengeTil { this.fom ?: YearMonth.now() }
}

fun Iterable<Kompetanse>?.settFomOgTom(periode: Periode<*, Måned>) =
    this?.map { kompetanse -> kompetanse.settFomOgTom(periode) }

fun Kompetanse.settFomOgTom(periode: Periode<*, Måned>) =
    this.copy(
        fom = periode.fraOgMed.tilYearMonthEllerNull(),
        tom = periode.tilOgMed.tilYearMonthEllerNull()
    )
