package no.nav.familie.ba.sak.integrasjoner.ecb.domene

import java.math.BigDecimal
import java.time.LocalDate

data class ExchangeRate(val currency: String, val exchangeRate: BigDecimal, val date: LocalDate)

fun List<ExchangeRate>.exchangeRateForCurrency(valuta: String): ExchangeRate {
    return this.first { it.currency == valuta }
}
