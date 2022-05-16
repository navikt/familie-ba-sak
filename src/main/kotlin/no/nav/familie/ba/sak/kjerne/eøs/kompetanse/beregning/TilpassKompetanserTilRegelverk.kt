package no.nav.familie.ba.sak.kjerne.eøs.kompetanse.beregning

import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilpassSkjemaerTilBarnasTidslinjer
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
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
): Collection<Kompetanse> = tilpassSkjemaerTilBarnasTidslinjer(
    kompetanser,
    barnTilRegelverkTidslinjer,
    manglendeKompetanseMapKombinator,
    overflødigKompetanseMapKombinator
)
