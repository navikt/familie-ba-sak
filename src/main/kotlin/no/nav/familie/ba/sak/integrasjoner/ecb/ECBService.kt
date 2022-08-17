package no.nav.familie.ba.sak.integrasjoner.ecb

import no.nav.familie.ba.sak.integrasjoner.ecb.domene.ECBExchangeRatesData
import no.nav.familie.ba.sak.integrasjoner.ecb.domene.exchangeRatesForCurrency
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import kotlin.jvm.Throws

@Service
class ECBService(val ecbClient: ECBClient) {

    /**
     * @param utenlandskValuta valutaen vi skal konvertere til NOK
     * @param kursDato datoen vi skal hente valutakurser for
     * @return Henter valutakurs for *utenlandskValuta* -> EUR og NOK -> EUR på *kursDato*, og returnerer en beregnet kurs for *utenlandskValuta* -> NOK.
     */
    @Throws(ECBClientException::class)
    fun hentValutakurs(utenlandskValuta: String, kursDato: LocalDate): BigDecimal {
        val ecbExchangeRatesData = ecbClient.getECBExchangeRatesData(utenlandskValuta, kursDato)
        val valutakursNOK = ecbExchangeRatesData.valutakursForValuta(ECBConstants.NOK)
        if (utenlandskValuta == ECBConstants.EUR) {
            return valutakursNOK
        }
        val valutakursUtenlandskValuta = ecbExchangeRatesData.valutakursForValuta(utenlandskValuta)
        return beregnValutakurs(valutakursUtenlandskValuta, valutakursNOK)
    }

    private fun beregnValutakurs(valutakursUtenlandskValuta: BigDecimal, valutakursNOK: BigDecimal): BigDecimal {
        return valutakursNOK.divide(valutakursUtenlandskValuta, 10, RoundingMode.HALF_UP)
    }

    private fun ECBExchangeRatesData.valutakursForValuta(valuta: String): BigDecimal {
        return this.exchangeRatesForCurrency(valuta).first().value
    }
}
