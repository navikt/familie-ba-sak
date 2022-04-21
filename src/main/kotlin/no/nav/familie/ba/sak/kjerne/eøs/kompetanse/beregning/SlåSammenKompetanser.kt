package no.nav.familie.ba.sak.kjerne.eøs.kompetanse.beregning

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.utenBarn
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.utenPeriode
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.komprimer
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.snittKombinerUtenNull
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned

fun Collection<Kompetanse>.slåSammen(): Collection<Kompetanse> {

    if (this.isEmpty())
        return this

    val kompetanseTidslinjer = this.map { KompetanseTidslinje(it) }
    val kompetanseSettTidslinje: Tidslinje<Set<Kompetanse>, Måned> = kompetanseTidslinjer
        .snittKombinerUtenNull {
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

private fun Kompetanse.leggSammenBarn(kompetanse: Kompetanse) =
    this.copy(barnAktører = this.barnAktører + kompetanse.barnAktører)

fun Iterable<Kompetanse>?.settFomOgTom(periode: Periode<*, Måned>) =
    this?.map { kompetanse -> kompetanse.settFomOgTom(periode) }

fun Kompetanse.settFomOgTom(periode: Periode<*, Måned>) =
    this.copy(
        fom = periode.fraOgMed.tilYearMonthEllerNull(),
        tom = periode.tilOgMed.tilYearMonthEllerNull()
    )
