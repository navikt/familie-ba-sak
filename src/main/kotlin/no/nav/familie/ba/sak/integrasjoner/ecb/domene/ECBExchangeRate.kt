package no.nav.familie.ba.sak.integrasjoner.ecb.domene
import org.simpleframework.xml.Attribute
import org.simpleframework.xml.Path
import org.simpleframework.xml.Root
import java.math.BigDecimal
import java.time.LocalDate

@Root(name = "Obs", strict = false)
class ECBExchangeRate {
    @field:Path("ObsDimension")
    @field:Attribute(name = "value")
    lateinit var date: String

    @field:Path("ObsValue")
    @field:Attribute(name = "value")
    lateinit var value: BigDecimal
}
