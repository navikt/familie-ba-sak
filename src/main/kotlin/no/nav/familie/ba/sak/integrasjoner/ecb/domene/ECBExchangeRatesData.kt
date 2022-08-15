package no.nav.familie.ba.sak.integrasjoner.ecb.domene

import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

@Root(strict = false, name = "DataSet")
class ECBExchangeRatesData {
    @field:ElementList(name = "Series", required = false, inline = true)
    lateinit var exchangeRatesForContries: List<ECBExchangeRatesForCountry>
}
