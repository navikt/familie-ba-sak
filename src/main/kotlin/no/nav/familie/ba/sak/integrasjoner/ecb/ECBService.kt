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

    @Throws(ECBClientException::class)
    fun hentValutakurs(fraValuta: String, valutaDato: LocalDate): BigDecimal {
        val ecbExchangeRatesData = ecbClient.hentValutakurs(fraValuta, valutaDato)
        val utenlandskValutakursTilEuro = ecbExchangeRatesData.valutakursForValuta(fraValuta)
        val norskValutakursTilEuro = ecbExchangeRatesData.valutakursForValuta("NOK")
        return beregnValutakurs(utenlandskValutakursTilEuro, norskValutakursTilEuro)
    }

    private fun beregnValutakurs(utenlandskValutakursTilEuro: BigDecimal, norskValutakursTilEuro: BigDecimal): BigDecimal {
        return norskValutakursTilEuro.divide(utenlandskValutakursTilEuro, 10, RoundingMode.HALF_UP).setScale(4, RoundingMode.HALF_UP)
    }

    private fun ECBExchangeRatesData.valutakursForValuta(valuta: String): BigDecimal {
        return this.exchangeRatesForCurrency(valuta).first().value
    }
}
