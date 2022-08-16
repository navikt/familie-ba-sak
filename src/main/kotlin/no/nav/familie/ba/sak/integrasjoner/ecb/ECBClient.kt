package no.nav.familie.ba.sak.integrasjoner.ecb

import no.nav.familie.ba.sak.common.kallEksternTjeneste
import no.nav.familie.ba.sak.integrasjoner.ecb.domene.ECBExchangeRatesData
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonException
import no.nav.familie.http.client.AbstractRestClient
import org.simpleframework.xml.Serializer
import org.simpleframework.xml.core.Persister
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.time.LocalDate
import kotlin.jvm.Throws

@Service
class ECBClient(val restTemplate: RestTemplate, @Value("\${FAMILIE_ECB_API_URL}") private val ecbBaseUrl: String) : AbstractRestClient(restTemplate, "familie-ecb") {

    @Throws(ECBClientException::class)
    fun hentValutakurs(fraValuta: String, valutakursDato: LocalDate): ECBExchangeRatesData {

        val uri = URI.create("${ecbBaseUrl}D.NOK+$fraValuta.EUR.SP00.A/?startPeriod=$valutakursDato&endPeriod=$valutakursDato")

        try {
            val ecbResponseString = kallEksternTjeneste("ECB", uri, "Hente valutakurser fra European Central Bank") {
                getForEntity<String>(uri, httpHeaders())
            }
            val serializer: Serializer = Persister()
            return serializer.read(ECBExchangeRatesData::class.java, ecbResponseString)
        } catch (e: IntegrasjonException) {
            throw ECBClientException("Feil ved henting av valutakurs fra ECB", e)
        }
    }

    private fun httpHeaders() =
        HttpHeaders().apply {
            contentType = MediaType.APPLICATION_XML
            accept = listOf(MediaType.APPLICATION_XML)
        }
}
