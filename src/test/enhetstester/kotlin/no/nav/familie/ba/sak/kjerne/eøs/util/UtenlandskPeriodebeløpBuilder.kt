package no.nav.familie.ba.sak.kjerne.eøs.util

import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan

class UtenlandskPeriodebeløpBuilder(
    startMåned: Tidspunkt<Måned> = jan(2020),
    behandlingId: Long = 1
) : SkjemaBuilder<UtenlandskPeriodebeløp, UtenlandskPeriodebeløpBuilder>(startMåned, behandlingId) {
    fun medBeløp(k: String, valutakode: String?, vararg barn: Person) = medSkjema(k, barn.toList()) {
        when {
            it == '-' -> UtenlandskPeriodebeløp.NULL
            it?.isDigit() ?: false -> {
                UtenlandskPeriodebeløp.NULL.copy(
                    beløp = it?.digitToInt()?.toBigDecimal(),
                    valutakode = valutakode,
                    intervall = "MND"
                )
            }
            else -> null
        }
    }
}
