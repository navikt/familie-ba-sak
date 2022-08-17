package no.nav.familie.ba.sak.integrasjoner.ecb

import no.nav.familie.ba.sak.common.kallEksternTjeneste
import no.nav.familie.ba.sak.integrasjoner.ecb.domene.ECBExchangeRatesData
import no.nav.familie.ba.sak.integrasjoner.ecb.domene.exchangeRatesForCurrency
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
    fun getECBExchangeRatesData(currency: String, exchangeRateDate: LocalDate): ECBExchangeRatesData {
        val uri = URI.create("${ecbBaseUrl}D.NOK${toCurrencyParam(currency)}.EUR.SP00.A/?startPeriod=$exchangeRateDate&endPeriod=$exchangeRateDate")

        try {
            val ecbResponseString = kallEksternTjeneste("ECB", uri, "Hente valutakurser fra European Central Bank") {
                getForEntity<String>(uri, httpHeaders())
            }
            val serializer: Serializer = Persister()
            val response = serializer.read(ECBExchangeRatesData::class.java, ecbResponseString)
            return validateResponse(currency, exchangeRateDate, response)
        } catch (e: IntegrasjonException) {
            throw ECBClientException("Feil ved henting av valutakurs fra ECB", e)
        }
    }

    private fun validateResponse(utenlandskValuta: String, valutakursDato: LocalDate, ecbExchangeRatesData: ECBExchangeRatesData): ECBExchangeRatesData {
        val errorMessage = "Fant ikke nødvendige valutakurser for utenlandsk valuta: $utenlandskValuta med dato: $valutakursDato"
        if (!isEUR(utenlandskValuta)) {
            if (ecbExchangeRatesData.exchangeRatesForCountries.size != 2 || ecbExchangeRatesData.exchangeRatesForCountries.distinctBy { it.ecbExchangeRateKey.currency }.size != 2) {
                throw ECBClientException(errorMessage)
            }
        } else if (ecbExchangeRatesData.exchangeRatesForCountries.size != 1 || ecbExchangeRatesData.exchangeRatesForCurrency(ECBConstants.NOK).size != 1) {
            throw ECBClientException(errorMessage)
        }
        return ecbExchangeRatesData
    }

    private fun httpHeaders() =
        HttpHeaders().apply {
            contentType = MediaType.APPLICATION_XML
            accept = listOf(MediaType.APPLICATION_XML)
        }

    private fun toCurrencyParam(currency: String): String {
        if (!isEUR(currency)) {
            return "+$currency"
        }
        return ""
    }

    private fun isEUR(currency: String): Boolean = currency == ECBConstants.EUR
}
