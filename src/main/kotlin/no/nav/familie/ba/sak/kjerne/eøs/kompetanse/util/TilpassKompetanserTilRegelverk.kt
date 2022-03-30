package no.nav.familie.ba.sak.kjerne.eøs.kompetanse.util

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.AktørKompetanseTidslinje
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerUtvendigMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk

val overflødigKompetanseMapKombinator = { _: Aktør ->
    { kompetanse: Kompetanse?, regelverk: Regelverk? ->
        if (regelverk != Regelverk.EØS_FORORDNINGEN && kompetanse != null) kompetanse else null
    }
}

val manglendeKompetanseMapKombinator = { aktør: Aktør ->
    { kompetanse: Kompetanse?, regelverk: Regelverk? ->
        if (regelverk == Regelverk.EØS_FORORDNINGEN && kompetanse == null)
            Kompetanse(fom = null, tom = null, barnAktører = setOf(aktør))
        else null
    }
}

fun tilpassKompetanserTilRegelverk(
    kompetanser: Collection<Kompetanse>,
    barnTilRegelverkTidslinjer: Map<Aktør, Tidslinje<Regelverk, Måned>>
): Collection<Kompetanse> {
    val kompetanserUtenOverflødige = fjernOverflødigeKompetanserRekursivt(kompetanser, barnTilRegelverkTidslinjer)

    val barnTilKompetanseTidslinje = kompetanserUtenOverflødige.tilTidslinjerForBarna()

    val manglendeKompetanser = barnTilKompetanseTidslinje
        .kombinerMed(barnTilRegelverkTidslinjer, manglendeKompetanseMapKombinator)
        .slåSammen()

    return (kompetanserUtenOverflødige + manglendeKompetanser).slåSammen()
}

fun fjernOverflødigeKompetanserRekursivt(
    kompetanser: Collection<Kompetanse>,
    barnTilRegelverkTidslinjer: Map<Aktør, Tidslinje<Regelverk, Måned>>
): Collection<Kompetanse> {
    val barnTilKompetanseTidslinje = kompetanser.tilTidslinjerForBarna()

    val overflødigeKompetanser = barnTilKompetanseTidslinje
        .kombinerUtvendigMed(barnTilRegelverkTidslinjer, overflødigKompetanseMapKombinator, MånedTidspunkt.nå())
        .slåSammen()

    return if (overflødigeKompetanser.isNotEmpty()) {
        val kompetanserUtenOverflødig = kompetanser.trekkFra(overflødigeKompetanser.first())
        fjernOverflødigeKompetanserRekursivt(kompetanserUtenOverflødig, barnTilRegelverkTidslinjer)
    } else {
        kompetanser
    }
}

fun Iterable<Kompetanse>.tilTidslinjerForBarna(): Map<Aktør, Tidslinje<Kompetanse, Måned>> {
    if (this.toList().isEmpty()) return emptyMap()

    val alleBarnAktørIder = this.map { it.barnAktører }.reduce { akk, neste -> akk + neste }

    return alleBarnAktørIder.associateWith { aktør ->
        AktørKompetanseTidslinje(aktør, this.filter { it.barnAktører.contains(aktør) })
    }
}

fun Map<Aktør, Tidslinje<Kompetanse, Måned>>.slåSammen() =
    this.flatMap { (_, tidslinjer) -> tidslinjer.tilKompetanser() }
        .slåSammen()

fun Tidslinje<Kompetanse, Måned>.tilKompetanser() =
    this.perioder().map { periode ->
        periode.innhold?.settFomOgTom(periode)
    }.filterNotNull()
