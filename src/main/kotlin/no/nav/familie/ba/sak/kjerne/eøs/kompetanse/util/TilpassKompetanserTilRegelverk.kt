package no.nav.familie.ba.sak.kjerne.eøs.kompetanse.util

import no.nav.familie.ba.sak.kjerne.beregning.AktørId
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.AktørKompetanseTidslinje
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk

val overflødigKompetanseMapKombinator = { _: AktørId ->
    { kompetanse: Kompetanse?, regelverk: Regelverk? ->
        if (regelverk != Regelverk.EØS_FORORDNINGEN && kompetanse != null) kompetanse else null
    }
}

val manglendeKompetanseMapKombinator = { aktørId: AktørId ->
    { kompetanse: Kompetanse?, regelverk: Regelverk? ->
        if (regelverk == Regelverk.EØS_FORORDNINGEN && kompetanse == null)
            Kompetanse(fom = null, tom = null, barnAktørIder = setOf(aktørId))
        else null
    }
}

fun tilpassKompetanserTilRegelverk(
    kompetanser: Collection<Kompetanse>,
    barnTilRegelverkTidslinjer: Map<AktørId, Tidslinje<Regelverk, Måned>>
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
    barnTilRegelverkTidslinjer: Map<AktørId, Tidslinje<Regelverk, Måned>>
): Collection<Kompetanse> {
    val barnTilKompetanseTidslinje = kompetanser.tilTidslinjerForBarna()

    val overflødigeKompetanser = barnTilKompetanseTidslinje
        .kombinerMed(barnTilRegelverkTidslinjer, overflødigKompetanseMapKombinator)
        .slåSammen()

    return if (overflødigeKompetanser.isNotEmpty()) {
        val kompetanserUtenOverflødig = kompetanser.trekkFra(overflødigeKompetanser.first())
        fjernOverflødigeKompetanserRekursivt(kompetanserUtenOverflødig, barnTilRegelverkTidslinjer)
    } else {
        kompetanser
    }
}

fun Iterable<Kompetanse>.tilTidslinjerForBarna(): Map<AktørId, Tidslinje<Kompetanse, Måned>> {
    val alleBarnAktørIder = this.map { it.barnAktørIder }.reduce { akk, neste -> akk + neste }

    return alleBarnAktørIder.associateWith { aktørId ->
        this.filter { it.barnAktørIder.contains(aktørId) }
            .let { AktørKompetanseTidslinje(aktørId, it) }
    }
}

fun Map<AktørId, Tidslinje<Kompetanse, Måned>>.slåSammen() =
    this.flatMap { (_, tidslinjer) -> tidslinjer.tilKompetanser() }
        .slåSammen()

fun Tidslinje<Kompetanse, Måned>.tilKompetanser() =
    this.perioder().map { periode ->
        periode.innhold?.settFomOgTom(periode)
    }.filterNotNull()
