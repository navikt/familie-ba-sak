package no.nav.familie.ba.sak.kjerne.eøs.valutakurs

import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilSeparateTidslinjerForBarna
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilSkjemaer
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilpassTil
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp

internal fun tilpassValutakurserTilUtenlandskePeriodebeløp(
    forrigeValutakurser: Collection<Valutakurs>,
    gjeldendeUtenlandskePeriodebeløp: Collection<UtenlandskPeriodebeløp>
): Collection<Valutakurs> {
    val barnasUtenlandskePeriodebeløpTidslinjer = gjeldendeUtenlandskePeriodebeløp
        .tilSeparateTidslinjerForBarna()

    return forrigeValutakurser.tilSeparateTidslinjerForBarna()
        .tilpassTil(barnasUtenlandskePeriodebeløpTidslinjer) { valutakurs, utenlandskPeriodebeløp ->
            when {
                valutakurs == null || valutakurs.valutakode != utenlandskPeriodebeløp.valutakode ->
                    Valutakurs.NULL.copy(valutakode = utenlandskPeriodebeløp.valutakode)
                else -> valutakurs
            }
        }
        .tilSkjemaer()
}
