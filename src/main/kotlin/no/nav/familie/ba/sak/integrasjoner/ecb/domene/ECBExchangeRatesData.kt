package no.nav.familie.ba.sak.integrasjoner.ecb.domene

import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Path
import org.simpleframework.xml.Root
import java.time.LocalDate

@Root(strict = false, name = "GenericData")
class ECBExchangeRatesData {
    @field:Path("DataSet")
    @field:ElementList(name = "Series", inline = true)
    lateinit var exchangeRatesForCurrencies: List<ECBExchangeRatesForCurrency>
}

fun ECBExchangeRatesData.exchangeRatesForCurrency(valuta: String): List<ECBExchangeRate> {
    return this.exchangeRatesForCurrencies.filter {
        it.ecbExchangeRateKey.currency == valuta
    }.flatMap { it.exchangeRates }
}

fun ECBExchangeRatesData.toExchangeRates(): List<ExchangeRate> {
    return this.exchangeRatesForCurrencies
        .flatMap { ecbExchangeRatesForCurrency ->
            ecbExchangeRatesForCurrency.exchangeRates
                .map { ecbExchangeRate ->
                    ExchangeRate(ecbExchangeRatesForCurrency.ecbExchangeRateKey.currency, ecbExchangeRate.value, LocalDate.parse(ecbExchangeRate.date))
                }
        }
}
