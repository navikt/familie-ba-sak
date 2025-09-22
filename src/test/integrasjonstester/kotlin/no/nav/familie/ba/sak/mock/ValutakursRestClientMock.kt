package no.nav.familie.ba.sak.mock

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.integrasjoner.ecb.ECBConstants
import no.nav.familie.valutakurs.ValutakursRestClient
import no.nav.familie.valutakurs.domene.ExchangeRate
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.math.BigDecimal
import java.time.LocalDate

@TestConfiguration
class ValutakursRestClientMock {
    @Bean
    @Primary
    fun mockValutakursRestClientMock(): ValutakursRestClient {
        val valutakursRestClientMock = mockk<ValutakursRestClient>()

        clearValutakursRestClient(valutakursRestClientMock)

        return valutakursRestClientMock
    }

    companion object {
        fun clearValutakursRestClient(valutakursRestClientMock: ValutakursRestClient) {
            clearMocks(valutakursRestClientMock)

            every {
                valutakursRestClientMock.hentValutakurs(
                    frequency = any(),
                    currencies = any(),
                    exchangeRateDate = any(),
                )
            } answers {
                val currencies = secondArg<List<String>>()
                val dagensDato = thirdArg<LocalDate>()
                val exchangeRates =
                    currencies.map {
                        ExchangeRate(
                            currency = it,
                            exchangeRate = BigDecimal(10),
                            date = dagensDato,
                        )
                    }
                if (ECBConstants.EUR in currencies) {
                    exchangeRates.filter { it.currency == ECBConstants.NOK }
                } else {
                    exchangeRates
                }
            }
        }
    }
}
