package no.nav.familie.ba.sak.integrasjoner.ecb

import no.nav.familie.ba.sak.integrasjoner.ecb.domene.ECBExchangeRatesData
import org.simpleframework.xml.core.Persister

class ECBXmlParser {

    companion object {
        fun parse(xmlString: String): ECBExchangeRatesData {
            return Persister().read(ECBExchangeRatesData::class.java, xmlString)
        }
    }
}
