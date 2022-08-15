package no.nav.familie.ba.sak.integrasjoner.ecb.domene

import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

@Root(name = "Series", strict = false)
class ECBExchangeRatesForCountry {

    @field:Element(name = "SeriesKey", required = false)
    var exchangeRateKey: ECBExchangeRateKey? = null

    @field:Element(name = "Attributes", required = false)
    var exchangeRateAttributes: ECBExchangeRateAttributes? = null

    @field:ElementList(name = "Obs", required = false, inline = true)
    lateinit var exchangeRates: List<ECBExchangeRate>
}
