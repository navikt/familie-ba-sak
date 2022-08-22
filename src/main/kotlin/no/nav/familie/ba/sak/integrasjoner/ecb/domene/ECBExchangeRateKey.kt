package no.nav.familie.ba.sak.integrasjoner.ecb.domene

import org.simpleframework.xml.Attribute
import org.simpleframework.xml.Path
import org.simpleframework.xml.Root

@Root(name = "SeriesKey", strict = false)
class ECBExchangeRateKey {
    @field:Path(value = "Value[1]")
    @field:Attribute(name = "value")
    lateinit var frequency: String

    @field:Path(value = "Value[2]")
    @field:Attribute(name = "value")
    lateinit var currency: String
}
