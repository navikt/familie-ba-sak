package no.nav.familie.ba.sak.integrasjoner.ecb.domene
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import java.time.LocalDate

@JacksonXmlRootElement(localName = "GenericData")
data class ECBExchangeRatesData(
    @field:JacksonXmlProperty(localName = "DataSet")
    val ecbExchangeRatesDataSet: ECBExchangeRatesDataSet
)

fun ECBExchangeRatesData.exchangeRatesForCurrency(valuta: String): List<ECBExchangeRate> {
    return this.ecbExchangeRatesDataSet.ecbExchangeRatesForCurrencies.filter {
        it.ecbExchangeRateKeys.any {
                ecbKeyValue ->
            ecbKeyValue.id == "CURRENCY" && ecbKeyValue.value == valuta
        }
    }.flatMap { it.ecbExchangeRates }
}

fun ECBExchangeRatesData.toExchangeRates(): List<ExchangeRate> {
    return this.ecbExchangeRatesDataSet.ecbExchangeRatesForCurrencies
        .flatMap { ecbExchangeRatesForCurrency ->
            ecbExchangeRatesForCurrency.ecbExchangeRates
                .map { ecbExchangeRate ->
                    val currency = ecbExchangeRatesForCurrency.ecbExchangeRateKeys.first { it.id == "CURRENCY" }.value
                    ExchangeRate(currency, ecbExchangeRate.ecbExchangeRateValue.value, LocalDate.parse(ecbExchangeRate.date.value))
                }
        }
}
