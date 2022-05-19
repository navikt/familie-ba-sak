package no.nav.familie.ba.sak.kjerne.eøs.util

import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan

class ValutakursBuilder(
    startMåned: Tidspunkt<Måned> = jan(2020),
    behandlingId: Long = 1
) : SkjemaBuilder<Valutakurs, ValutakursBuilder>(startMåned, behandlingId) {
    fun medKurs(k: String, valutakode: String, vararg barn: Person) = medSkjema(k, barn.toList()) {
        when {
            it == '-' -> Valutakurs.NULL
            it?.isDigit() ?: false -> {
                Valutakurs.NULL.copy(
                    kurs = it?.digitToInt()?.toBigDecimal(),
                    valutakode = valutakode,
                    valutakursdato = null
                )
            }
            else -> null
        }
    }
}
