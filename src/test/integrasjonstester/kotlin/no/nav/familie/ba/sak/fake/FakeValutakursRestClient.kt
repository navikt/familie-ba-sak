package no.nav.familie.ba.sak.fake

import no.nav.familie.ba.sak.integrasjoner.ecb.ECBConstants
import no.nav.familie.valutakurs.Frequency
import no.nav.familie.valutakurs.ValutakursRestClient
import no.nav.familie.valutakurs.domene.ExchangeRate
import org.springframework.web.client.RestOperations
import java.math.BigDecimal
import java.time.LocalDate

class FakeValutakursRestClient(
    restOperations: RestOperations,
) : ValutakursRestClient(restOperations = restOperations) {
    override fun hentValutakurs(
        frequency: Frequency,
        currencies: List<String>,
        exchangeRateDate: LocalDate,
    ): List<ExchangeRate> {
        val exchangeRates =
            currencies.map {
                ExchangeRate(
                    currency = it,
                    exchangeRate = BigDecimal(10),
                    date = exchangeRateDate,
                )
            }
        return if (ECBConstants.EUR in currencies) {
            exchangeRates.filter { it.currency == ECBConstants.NOK }
        } else {
            exchangeRates
        }
    }
}
