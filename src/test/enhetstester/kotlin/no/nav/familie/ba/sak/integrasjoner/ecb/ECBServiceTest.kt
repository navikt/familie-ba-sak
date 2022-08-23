package no.nav.familie.ba.sak.integrasjoner.ecb

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.config.restTemplate
import no.nav.familie.ba.sak.integrasjoner.ecb.domene.ECBExchangeRate
import no.nav.familie.ba.sak.integrasjoner.ecb.domene.ECBExchangeRateDate
import no.nav.familie.ba.sak.integrasjoner.ecb.domene.ECBExchangeRateKey
import no.nav.familie.ba.sak.integrasjoner.ecb.domene.ECBExchangeRateValue
import no.nav.familie.ba.sak.integrasjoner.ecb.domene.ECBExchangeRatesData
import no.nav.familie.ba.sak.integrasjoner.ecb.domene.ECBExchangeRatesDataSet
import no.nav.familie.ba.sak.integrasjoner.ecb.domene.ECBExchangeRatesForCurrency
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate

class ECBServiceTest {

    @Test
    fun `Hent valutakurs for utenlandsk valuta til NOK og sjekk at beregning av kurs er riktig`() {
        val ecbService = ECBService(ECBClient(restTemplate, "https://sdw-wsrest.ecb.europa.eu/service/data/EXR/"))
        val valutakursDato = LocalDate.of(2022, 6, 28)
        val SEKtilNOKValutakurs = ecbService.hentValutakurs("SEK", valutakursDato)
        assertEquals(BigDecimal.valueOf(0.9702185972), SEKtilNOKValutakurs)
    }

    @Test
    fun `Test at ECBClient kaster ECBClientException dersom man forsøker å hente kurser for helgedatoer og får tom respons fra ECB`() {
        val ecbClient = ECBClient(restTemplate, "https://sdw-wsrest.ecb.europa.eu/service/data/EXR/")
        val ecbService = ECBService(ecbClient)
        val valutakursDatoLørdag = LocalDate.of(2022, 7, 23)
        val valutakursDatoSøndag = valutakursDatoLørdag.plusDays(1)
        assertThrows<ECBClientException> { ecbService.hentValutakurs("SEK", valutakursDatoLørdag) }
        assertThrows<ECBClientException> { ecbService.hentValutakurs("SEK", valutakursDatoSøndag) }
    }

    @Test
    fun `Test at ECBService kaster ESBServiceException dersom de returnerte kursene ikke inneholder kurs for forespurt valuta`() {
        val ecbClient = mockk<ECBClient>()
        val ecbService = ECBService(ecbClient)
        val valutakursDato = LocalDate.of(2022, 7, 22)
        val ecbExchangeRatesData = ECBExchangeRatesData(
            ECBExchangeRatesDataSet(
                listOf(
                    ECBExchangeRatesForCurrency(
                        listOf(ECBExchangeRateKey("CURRENCY", "NOK")), listOf(ECBExchangeRate(ECBExchangeRateDate("2022-07-23"), ECBExchangeRateValue(BigDecimal.valueOf(9.4567))))
                    )
                )
            )
        )
        every { ecbClient.getExchangeRates("SEK", valutakursDato) } returns ecbExchangeRatesData
        assertThrows<ECBServiceException> { ecbService.hentValutakurs("SEK", valutakursDato) }
    }

    @Test
    fun `Test at ECBService kaster ESBServiceException dersom de returnerte kursene ikke inneholder kurser med forespurt dato`() {
        val ecbClient = mockk<ECBClient>()
        val ecbService = ECBService(ecbClient)
        val valutakursDato = LocalDate.of(2022, 7, 20)
        val ecbExchangeRatesData = ECBExchangeRatesData(
            ECBExchangeRatesDataSet(
                listOf(
                    ECBExchangeRatesForCurrency(
                        listOf(ECBExchangeRateKey("CURRENCY", "NOK")), listOf(ECBExchangeRate(ECBExchangeRateDate("2022-07-21"), ECBExchangeRateValue(BigDecimal.valueOf(9.4567))))
                    ),
                    ECBExchangeRatesForCurrency(
                        listOf(ECBExchangeRateKey("CURRENCY", "SEK")), listOf(ECBExchangeRate(ECBExchangeRateDate("2022-07-21"), ECBExchangeRateValue(BigDecimal.valueOf(9.4567))))
                    )
                )
            )
        )
        every { ecbClient.getExchangeRates("SEK", valutakursDato) } returns ecbExchangeRatesData
        assertThrows<ECBServiceException> { ecbService.hentValutakurs("SEK", valutakursDato) }
    }

    @Test
    fun `Test at ECBService returnerer NOK til EUR dersom den forespurte valutaen er EUR`() {
        val ecbClient = mockk<ECBClient>()
        val ecbService = ECBService(ecbClient)
        val nokTilEur = BigDecimal.valueOf(9.4567)
        val valutakursDato = LocalDate.of(2022, 7, 20)
        val ecbExchangeRatesData = ECBExchangeRatesData(
            ECBExchangeRatesDataSet(
                listOf(
                    ECBExchangeRatesForCurrency(
                        listOf(ECBExchangeRateKey("CURRENCY", "NOK")), listOf(ECBExchangeRate(ECBExchangeRateDate("2022-07-20"), ECBExchangeRateValue(nokTilEur)))
                    )
                )
            )
        )
        every { ecbClient.getExchangeRates("EUR", valutakursDato) } returns ecbExchangeRatesData
        assertEquals(nokTilEur, ecbService.hentValutakurs("EUR", valutakursDato))
    }
}
