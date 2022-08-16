package no.nav.familie.ba.sak.integrasjoner.ecb.domene

import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Path
import org.simpleframework.xml.Root

@Root(strict = false, name = "GenericData")
class ECBExchangeRatesData {
    @field:Path("DataSet")
    @field:ElementList(name = "Series", inline = true)
    lateinit var exchangeRatesForCountries: List<ECBExchangeRatesForCountry>
}

fun ECBExchangeRatesData.exchangeRatesForCurrency(valuta: String): List<ECBExchangeRate> {
    return this.exchangeRatesForCountries.filter {
        it.ecbExchangeRateKey.currency == valuta
    }.flatMap { it.exchangeRates }
}
