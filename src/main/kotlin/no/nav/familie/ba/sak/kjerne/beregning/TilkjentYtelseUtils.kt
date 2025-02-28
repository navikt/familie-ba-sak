package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import java.math.BigDecimal
import java.time.YearMonth


internal data class BeregnetAndel(
    val person: Person,
    val stønadFom: YearMonth,
    val stønadTom: YearMonth,
    val beløp: Int,
    val sats: Int,
    val prosent: BigDecimal,
)