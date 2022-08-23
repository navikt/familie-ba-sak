package no.nav.familie.ba.sak.integrasjoner.ecb.domene
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import java.math.BigDecimal

data class ECBExchangeRate(
    @field:JacksonXmlProperty(localName = "ObsDimension")
    val date: ECBExchangeRateDate,

    @field:JacksonXmlProperty(localName = "ObsValue")
    val ecbExchangeRateValue: ECBExchangeRateValue
)

data class ECBExchangeRateDate(
    @field:JacksonXmlProperty(localName = "value", isAttribute = true)
    val value: String
)

data class ECBExchangeRateValue(
    @field:JacksonXmlProperty(localName = "value", isAttribute = true)
    val value: BigDecimal
)
