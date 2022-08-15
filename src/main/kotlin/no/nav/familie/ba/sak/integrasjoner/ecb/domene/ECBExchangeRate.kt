package no.nav.familie.ba.sak.integrasjoner.ecb.domene
import org.simpleframework.xml.Attribute
import org.simpleframework.xml.Element
import org.simpleframework.xml.Root

@Root(name = "Obs", strict = false)
class ECBExchangeRate {
    @field:Element(name = "ObsDimension", required = false)
    var date: ECBExchangeRateDate? = null
    @field:Element(name = "ObsValue", required = false)
    var value: ECBExchangeRateValue? = null
}

@Root(name = "ObsDimension", strict = false)
class ECBExchangeRateDate {
    @field:Attribute(name = "value", required = false)
    var date: String? = null
}

@Root(name = "ObsValue", strict = false)
class ECBExchangeRateValue {
    @field: Attribute(name = "value", required = false)
    var value: String? = null
}
