package no.nav.familie.ba.sak.integrasjoner.ecb.domene

import org.simpleframework.xml.Element
import org.simpleframework.xml.Root

@Root(strict = false, name = "GenericData")
class ECBExchangeRatesResult(
    @field:Element(name = "Header", required = false)
    var header: ECBExchangeRatesHeader? = null,
    @field:Element(name = "DataSet")
    var data: ECBExchangeRatesData? = null,
)
