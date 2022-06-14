package no.nav.familie.ba.sak.kjerne.eøs.util

import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan

class UtenlandskPeriodebeløpBuilder(
    startMåned: Tidspunkt<Måned> = jan(2020),
    behandlingId: BehandlingId = BehandlingId(1)
) : SkjemaBuilder<UtenlandskPeriodebeløp, UtenlandskPeriodebeløpBuilder>(startMåned, behandlingId) {
    fun medBeløp(k: String, valutakode: String?, utbetalingsland: String, vararg barn: Person) =
        medSkjema(k, barn.toList()) {
            when {
                it == '-' -> UtenlandskPeriodebeløp.NULL.copy(utbetalingsland = utbetalingsland)
                it == '$' -> UtenlandskPeriodebeløp.NULL.copy(
                    valutakode = valutakode,
                    utbetalingsland = utbetalingsland
                )
                it?.isDigit() ?: false -> {
                    UtenlandskPeriodebeløp.NULL.copy(
                        beløp = it?.digitToInt()?.toBigDecimal(),
                        valutakode = valutakode,
                        intervall = "MÅNEDLIG",
                        utbetalingsland = utbetalingsland
                    )
                }
                else -> null
            }
        }
}
