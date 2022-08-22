package no.nav.familie.ba.sak.integrasjoner.ecb

import no.nav.familie.ba.sak.integrasjoner.ecb.domene.ExchangeRate
import no.nav.familie.ba.sak.integrasjoner.ecb.domene.exchangeRateForCurrency
import no.nav.familie.ba.sak.integrasjoner.ecb.domene.toExchangeRates
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.jvm.Throws

@Service
class ECBService(val ecbClient: ECBClient) {

    /**
     * @param utenlandskValuta valutaen vi skal konvertere til NOK
     * @param kursDato datoen vi skal hente valutakurser for
     * @return Henter valutakurs for *utenlandskValuta* -> EUR og NOK -> EUR på *kursDato*, og returnerer en beregnet kurs for *utenlandskValuta* -> NOK.
     */
    @Throws(ECBServiceException::class, ECBClientException::class)
    fun hentValutakurs(utenlandskValuta: String, kursDato: LocalDate): BigDecimal {
        val exchangeRates = ecbClient.getExchangeRates(utenlandskValuta, kursDato).toExchangeRates()
        validateExchangeRates(utenlandskValuta, kursDato, exchangeRates)
        val valutakursNOK = exchangeRates.exchangeRateForCurrency(ECBConstants.NOK)
        if (utenlandskValuta == ECBConstants.EUR) {
            return valutakursNOK.exchangeRate
        }
        val valutakursUtenlandskValuta = exchangeRates.exchangeRateForCurrency(utenlandskValuta)
        return beregnValutakurs(valutakursUtenlandskValuta.exchangeRate, valutakursNOK.exchangeRate)
    }

    private fun beregnValutakurs(valutakursUtenlandskValuta: BigDecimal, valutakursNOK: BigDecimal): BigDecimal {
        return valutakursNOK.divide(valutakursUtenlandskValuta, 10, RoundingMode.HALF_UP)
    }

    private fun validateExchangeRates(currency: String, exchangeRateDate: LocalDate, exchangeRates: List<ExchangeRate>) {
        if (currency != ECBConstants.EUR) {
            if (!isValid(exchangeRates, listOf(currency, ECBConstants.NOK), exchangeRateDate, 2)) {
                throwNotFound(currency, exchangeRateDate)
            }
        } else if (!isValid(exchangeRates, listOf(ECBConstants.NOK), exchangeRateDate, 1)) {
            throwNotFound(currency, exchangeRateDate)
        }
    }

    private fun isValid(exchangeRates: List<ExchangeRate>, currencies: List<String>, exchangeRateDate: LocalDate, expectedSize: Int): Boolean {
        return exchangeRates.size == expectedSize && exchangeRates.all { it.date == exchangeRateDate } && exchangeRates.map { it.currency }.containsAll(currencies)
    }

    private fun throwNotFound(currency: String, exchangeRateDate: LocalDate) {
        val dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val formattedExchangeRateDate = exchangeRateDate.format(dateTimeFormatter)
        throw ECBServiceException("Fant ikke nødvendige valutakurser for valutakursdato $formattedExchangeRateDate for å bestemme valutakursen $currency - NOK")
    }
}
