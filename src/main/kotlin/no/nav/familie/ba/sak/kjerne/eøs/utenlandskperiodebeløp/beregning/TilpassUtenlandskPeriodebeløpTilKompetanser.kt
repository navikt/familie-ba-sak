package no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.beregning

import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilpassSkjemaerTilBarnasTidslinjer
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned

val overflødigUtenlandskePeriodebeløpMapKombinator = { _: Aktør ->
    { utenlandskPeriodebeløp: UtenlandskPeriodebeløp?, kompetanse: Kompetanse? ->
        if (kompetanse == null && utenlandskPeriodebeløp != null)
            utenlandskPeriodebeløp
        else null
    }
}

val manglendeUtenlandskPeriodebeløpMapKombinator = { aktør: Aktør ->
    { utenlandskPeriodebeløp: UtenlandskPeriodebeløp?, kompetanse: Kompetanse? ->
        if (kompetanse != null && utenlandskPeriodebeløp == null)
            UtenlandskPeriodebeløp(fom = null, tom = null, barnAktører = setOf(aktør))
        else null
    }
}

fun tilpassUtenlandskePeriodebeløpTilKompetanser(
    utenlandskPeriodebeløp: Collection<UtenlandskPeriodebeløp>,
    barnTilKompetanseTidslinjer: Map<Aktør, Tidslinje<Kompetanse, Måned>>
): Collection<UtenlandskPeriodebeløp> = tilpassSkjemaerTilBarnasTidslinjer(
    utenlandskPeriodebeløp,
    barnTilKompetanseTidslinjer,
    manglendeUtenlandskPeriodebeløpMapKombinator,
    overflødigUtenlandskePeriodebeløpMapKombinator
)
