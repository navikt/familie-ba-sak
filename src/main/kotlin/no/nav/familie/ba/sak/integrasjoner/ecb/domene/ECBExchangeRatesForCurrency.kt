package no.nav.familie.ba.sak.integrasjoner.ecb.domene

import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

@Root(name = "Series", strict = false)
class ECBExchangeRatesForCurrency {

    @field:Element(name = "SeriesKey")
    lateinit var ecbExchangeRateKey: ECBExchangeRateKey

    @field:ElementList(name = "Obs", inline = true)
    lateinit var exchangeRates: List<ECBExchangeRate>
}
