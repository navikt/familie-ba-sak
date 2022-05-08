package no.nav.familie.ba.sak.kjerne.eøs.kompetanse.beregning

import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.settFomOgTom
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.slåSammen
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.trekkFra
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.tidslinjer.AktørKompetanseTidslinje
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerForAlleNøklerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
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
        .kombinerForAlleNøklerMed(barnTilRegelverkTidslinjer, manglendeKompetanseMapKombinator)
        .slåSammen()

    return (kompetanserUtenOverflødige + manglendeKompetanser).slåSammen()
}

fun fjernOverflødigeKompetanserRekursivt(
    kompetanser: Collection<Kompetanse>,
    barnTilRegelverkTidslinjer: Map<Aktør, Tidslinje<Regelverk, Måned>>
): Collection<Kompetanse> {
    val barnTilKompetanseTidslinje = kompetanser.tilTidslinjerForBarna()

    val overflødigeKompetanser = barnTilKompetanseTidslinje
        .kombinerForAlleNøklerMed(barnTilRegelverkTidslinjer, overflødigKompetanseMapKombinator)
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
    this.perioder().mapNotNull { periode ->
        periode.innhold?.settFomOgTom(periode)
    }
