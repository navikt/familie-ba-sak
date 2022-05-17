package no.nav.familie.ba.sak.kjerne.eøs.valutakurs.beregning

import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilpassSkjemaerTilBarnasTidslinjer
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned

val overflødigValutakurserMapKombinator = { _: Aktør ->
    { valutakurs: Valutakurs?, utenlandskPeriodebeløp: UtenlandskPeriodebeløp? ->
        if (utenlandskPeriodebeløp == null && valutakurs != null)
            valutakurs
        else null
    }
}

val manglendeValutakursMapKombinator = { aktør: Aktør ->
    { valutakurs: Valutakurs?, utenlandskPeriodebeløp: UtenlandskPeriodebeløp? ->
        if (utenlandskPeriodebeløp != null && valutakurs == null)
            Valutakurs(fom = null, tom = null, barnAktører = setOf(aktør))
        else null
    }
}

fun tilpassValutakurserTilUtenlandskePeriodebeløp(
    valutakurser: Collection<Valutakurs>,
    barnTilUtenlandskPeriodebeløpTidslinjer: Map<Aktør, Tidslinje<UtenlandskPeriodebeløp, Måned>>
): Collection<Valutakurs> = tilpassSkjemaerTilBarnasTidslinjer(
    valutakurser,
    barnTilUtenlandskPeriodebeløpTidslinjer,
    manglendeValutakursMapKombinator,
    overflødigValutakurserMapKombinator
)
