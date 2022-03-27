package no.nav.familie.ba.sak.kjerne.eøs.kompetanse.util

import no.nav.familie.ba.sak.kjerne.beregning.AktørId
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.AktørKompetanseTidslinje
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.minus
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinjer.TomTidslinje
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk

val overflødigKompetanseKombinator = { aktørId: AktørId ->
    { kompetanse: Kompetanse?, regelverk: Regelverk? ->
        if (regelverk != Regelverk.EØS_FORORDNINGEN && kompetanse != null) kompetanse else null
    }
}

val manglendeKompetanseKombinator = { aktørId: AktørId ->
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
    val overflødigeKompetanser = kompetanser.kombinerMed(barnTilRegelverkTidslinjer, overflødigKompetanseKombinator)
    val kompetanserUtenOverflødige = kompetanser.minus(overflødigeKompetanser)

    val manglendeKompetanser = kompetanser.kombinerMed(barnTilRegelverkTidslinjer, manglendeKompetanseKombinator)
    return (kompetanserUtenOverflødige + manglendeKompetanser).slåSammen()
}

fun Collection<Kompetanse>.kombinerMed(
    barnTilRegelverkTidslinje: Map<AktørId, Tidslinje<Regelverk, Måned>>,
    kombinator: (AktørId) -> (Kompetanse?, Regelverk?) -> Kompetanse?
): Collection<Kompetanse> {
    val alleBarnAktørIder = this.alleBarnasAktørIder() + barnTilRegelverkTidslinje.keys

    return alleBarnAktørIder.flatMap { aktørId ->
        val kompetanseTidslinje = this.tilTidslinjeforBarn(aktørId)
        val regelverkTidslinje = barnTilRegelverkTidslinje[aktørId] ?: TomTidslinje { MånedTidspunkt.nå() }
        kompetanseTidslinje.kombinerMed(regelverkTidslinje, kombinator(aktørId)).tilKompetanser()
    }.slåSammen()
}

fun Iterable<Kompetanse>.alleBarnasAktørIder() =
    this.map { it.barnAktørIder }.reduce { akk, neste -> akk + neste }

fun Iterable<Kompetanse>.tilTidslinjeforBarn(barnAktørId: AktørId) =
    this.filter { it.barnAktørIder.contains(barnAktørId) }
        .let { AktørKompetanseTidslinje(barnAktørId, it) }

fun Tidslinje<Kompetanse, Måned>.tilKompetanser() =
    this.perioder().map { periode ->
        periode.innhold?.settFomOgTom(periode)
    }.filterNotNull()
