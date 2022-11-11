package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.TidspunktClosedRange
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.tilYearMonth
import java.math.BigDecimal
import java.time.YearMonth

internal infix fun Person.får(prosent: Prosent) = BeregnetAndel(
    person = this,
    prosent = when (prosent) {
        Prosent.alt -> BigDecimal.valueOf(100)
        Prosent.halvparten -> BigDecimal.valueOf(50)
        Prosent.ingenting -> BigDecimal.ZERO
    },
    stønadFom = YearMonth.now(),
    stønadTom = YearMonth.now(),
    beløp = 0,
    sats = 0
)

@Suppress("ktlint:enum-entry-name-case")
enum class Prosent {
    alt,
    halvparten,
    ingenting
}

internal infix fun BeregnetAndel.av(sats: Int) = this.copy(
    sats = sats,
    beløp = prosent.multiply(sats.toBigDecimal()).toInt() / 100
)

internal infix fun BeregnetAndel.i(tidsrom: TidspunktClosedRange<Måned>) = this.copy(
    stønadFom = tidsrom.start.tilYearMonth(),
    stønadTom = tidsrom.endInclusive.tilYearMonth()
)
