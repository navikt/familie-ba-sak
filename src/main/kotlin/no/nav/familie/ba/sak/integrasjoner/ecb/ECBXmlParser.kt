package no.nav.familie.ba.sak.integrasjoner.ecb

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.familie.ba.sak.integrasjoner.ecb.domene.ECBExchangeRatesData

class ECBXmlParser {

    companion object {
        fun parse(xmlString: String): ECBExchangeRatesData {
            val mapper = XmlMapper()
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            mapper.registerKotlinModule()
            return mapper.readValue(xmlString, ECBExchangeRatesData::class.java)
        }
    }
}
