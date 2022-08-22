package no.nav.familie.ba.sak.integrasjoner.ecb

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.kallEksternTjeneste
import no.nav.familie.ba.sak.integrasjoner.ecb.domene.ECBExchangeRatesData
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonException
import no.nav.familie.http.client.AbstractRestClient
import org.simpleframework.xml.Serializer
import org.simpleframework.xml.core.Persister
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.jvm.Throws

@Service
class ECBClient(val restTemplate: RestTemplate, @Value("\${FAMILIE_ECB_API_URL}") private val ecbBaseUrl: String) : AbstractRestClient(restTemplate, "familie-ecb") {

    @Throws(ECBClientException::class)
    fun getExchangeRates(currency: String, exchangeRateDate: LocalDate): ECBExchangeRatesData {
        val uri = URI.create("${ecbBaseUrl}D.NOK${toCurrencyParam(currency)}.EUR.SP00.A/?startPeriod=$exchangeRateDate&endPeriod=$exchangeRateDate")

        try {
            val ecbResponseString: String? = kallEksternTjeneste("ECB", uri, "Hente valutakurser fra European Central Bank") {
                getForEntity(uri, httpHeaders())
            }
            if (!ecbResponseString.isNullOrEmpty()) {
                return ECBXmlParser.parse(ecbResponseString)
            }
            throw getECBClientException(currency, exchangeRateDate)
            // throwNotFound(currency, exchangeRateDate)
            // val serializer: Serializer = Persister()
            // return serializer.read(ECBExchangeRatesData::class.java, ecbResponseString)
        } catch (e: IntegrasjonException) {
            val feilmelding = "Teknisk feil ved henting av valutakurs fra European Central Bank"
            throw Feil(feilmelding, feilmelding, HttpStatus.INTERNAL_SERVER_ERROR, e, e.cause)
        }
    }

    private fun httpHeaders() =
        HttpHeaders().apply {
            contentType = MediaType.APPLICATION_XML
            accept = listOf(MediaType.APPLICATION_XML)
        }

    private fun toCurrencyParam(currency: String): String {
        if (currency != ECBConstants.EUR) {
            return "+$currency"
        }
        return ""
    }

    private fun getECBClientException(currency: String, exchangeRateDate: LocalDate) : ECBClientException{
        val dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val formattedExchangeRateDate = exchangeRateDate.format(dateTimeFormatter)
        return ECBClientException("Fant ingen valutakurser for $currency med kursdato $formattedExchangeRateDate")
    }
}
