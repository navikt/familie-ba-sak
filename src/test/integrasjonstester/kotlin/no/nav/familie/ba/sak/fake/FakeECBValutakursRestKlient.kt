package no.nav.familie.ba.sak.fake

import no.nav.familie.ba.sak.integrasjoner.ecb.ECBConstants
import no.nav.familie.valutakurs.ECBValutakursRestKlient
import no.nav.familie.valutakurs.domene.Valutakurs
import no.nav.familie.valutakurs.domene.ecb.Frequency
import org.springframework.web.client.RestOperations
import java.math.BigDecimal
import java.time.LocalDate

class FakeECBValutakursRestKlient(
    restOperations: RestOperations,
) : ECBValutakursRestKlient(restOperations = restOperations) {
    override fun hentValutakurs(
        frequency: Frequency,
        currencies: List<String>,
        exchangeRateDate: LocalDate,
    ): List<Valutakurs> {
        val exchangeRates =
            currencies.map {
                Valutakurs(
                    valuta = it,
                    kurs = BigDecimal(10),
                    kursDato = exchangeRateDate,
                )
            }
        return if (ECBConstants.EUR in currencies) {
            exchangeRates.filter { it.valuta == ECBConstants.NOK }
        } else {
            exchangeRates
        }
    }
}
