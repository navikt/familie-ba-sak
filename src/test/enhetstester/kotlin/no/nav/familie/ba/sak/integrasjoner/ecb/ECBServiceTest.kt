package no.nav.familie.ba.sak.integrasjoner.ecb

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.unmockkAll
import no.nav.familie.ba.sak.integrasjoner.ecb.domene.ECBValutakursCache
import no.nav.familie.ba.sak.integrasjoner.ecb.domene.ECBValutakursCacheRepository
import no.nav.familie.valutakurs.NorgesBankValutakursRestKlient
import no.nav.familie.valutakurs.ValutakursRestClient
import no.nav.familie.valutakurs.domene.ecb.ECBValutakursData
import no.nav.familie.valutakurs.domene.ecb.Frequency
import no.nav.familie.valutakurs.domene.ecb.toExchangeRates
import no.nav.familie.valutakurs.domene.sdmx.SDMXExchangeRate
import no.nav.familie.valutakurs.domene.sdmx.SDMXExchangeRateDate
import no.nav.familie.valutakurs.domene.sdmx.SDMXExchangeRateKey
import no.nav.familie.valutakurs.domene.sdmx.SDMXExchangeRateValue
import no.nav.familie.valutakurs.domene.sdmx.SDMXExchangeRatesDataSet
import no.nav.familie.valutakurs.domene.sdmx.SDMXExchangeRatesForCurrency
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ECBServiceTest {
    private val ecbClient = mockk<ValutakursRestClient>()
    private val ecbValutakursCacheRepository = mockk<ECBValutakursCacheRepository>()
    private val norgesBankValutakursRestKlient = mockk<NorgesBankValutakursRestKlient>(relaxed = true)

    private val ecbService = ECBService(ecbClient, norgesBankValutakursRestKlient, ecbValutakursCacheRepository)

    @AfterAll
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `Hent valutakurs for utenlandsk valuta til NOK og sjekk at beregning av kurs er riktig`() {
        val valutakursDato = LocalDate.of(2022, 6, 28)
        val valutakursNOK = BigDecimal.valueOf(10.337)
        val valutakursSEK = BigDecimal.valueOf(10.6543)
        val ecbExchangeRatesData =
            createECBResponse(
                Frequency.Daily,
                listOf(Pair("NOK", valutakursNOK), Pair("SEK", valutakursSEK)),
                valutakursDato.toString(),
            )

        every { ecbValutakursCacheRepository.findByValutakodeAndValutakursdato(any(), any()) } returns emptyList()
        every { ecbValutakursCacheRepository.save(any()) } answers { firstArg() }
        every {
            ecbClient.hentValutakurs(
                Frequency.Daily,
                listOf("NOK", "SEK"),
                valutakursDato,
            )
        } returns ecbExchangeRatesData.toExchangeRates()
        val sekTilNOKValutakurs = ecbService.hentValutakurs("SEK", valutakursDato)
        assertEquals(BigDecimal.valueOf(0.9702185972), sekTilNOKValutakurs)
    }

    @Test
    fun `Test at ECBService kaster ESBServiceException dersom de returnerte kursene ikke inneholder kurs for forespurt valuta`() {
        val valutakursDato = LocalDate.of(2022, 7, 22)
        val ecbExchangeRatesData =
            createECBResponse(
                Frequency.Daily,
                listOf(Pair("NOK", BigDecimal.valueOf(10.337))),
                valutakursDato.toString(),
            )
        every { ecbValutakursCacheRepository.findByValutakodeAndValutakursdato(any(), any()) } returns emptyList()
        every {
            ecbClient.hentValutakurs(
                Frequency.Daily,
                listOf("NOK", "SEK"),
                valutakursDato,
            )
        } returns ecbExchangeRatesData.toExchangeRates()
        assertThrows<ECBServiceException> { ecbService.hentValutakurs("SEK", valutakursDato) }
    }

    @Test
    fun `Test at ECBService kaster ESBServiceException dersom de returnerte kursene ikke inneholder kurser med forespurt dato`() {
        val valutakursDato = LocalDate.of(2022, 7, 20)
        val ecbExchangeRatesData =
            createECBResponse(
                Frequency.Daily,
                listOf(Pair("NOK", BigDecimal.valueOf(10.337)), Pair("SEK", BigDecimal.valueOf(10.6543))),
                valutakursDato.minusDays(1).toString(),
            )
        every { ecbValutakursCacheRepository.findByValutakodeAndValutakursdato(any(), any()) } returns emptyList()
        every {
            ecbClient.hentValutakurs(
                Frequency.Daily,
                listOf("NOK", "SEK"),
                valutakursDato,
            )
        } returns ecbExchangeRatesData.toExchangeRates()
        assertThrows<ECBServiceException> { ecbService.hentValutakurs("SEK", valutakursDato) }
    }

    @Test
    fun `Test at ECBService returnerer NOK til EUR dersom den forespurte valutaen er EUR`() {
        val nokTilEur = BigDecimal.valueOf(9.4567)
        val valutakursDato = LocalDate.of(2022, 7, 20)
        val ecbExchangeRatesData =
            createECBResponse(
                Frequency.Daily,
                listOf(Pair("NOK", BigDecimal.valueOf(9.4567))),
                valutakursDato.toString(),
            )
        every { ecbValutakursCacheRepository.findByValutakodeAndValutakursdato(any(), any()) } returns emptyList()
        every { ecbValutakursCacheRepository.save(any()) } returns ECBValutakursCache(kurs = BigDecimal.valueOf(9.4567), valutakode = "EUR", valutakursdato = valutakursDato)
        every {
            ecbClient.hentValutakurs(
                Frequency.Daily,
                listOf("NOK", "EUR"),
                valutakursDato,
            )
        } returns ecbExchangeRatesData.toExchangeRates()
        assertEquals(nokTilEur, ecbService.hentValutakurs("EUR", valutakursDato))
    }

    private fun createECBResponse(
        frequency: Frequency,
        exchangeRates: List<Pair<String, BigDecimal>>,
        exchangeRateDate: String,
    ): ECBValutakursData =
        ECBValutakursData(
            SDMXExchangeRatesDataSet(
                exchangeRates.map {
                    SDMXExchangeRatesForCurrency(
                        listOf(
                            SDMXExchangeRateKey("CURRENCY", it.first),
                            SDMXExchangeRateKey("FREQ", frequency.toFrequencyParam()),
                        ),
                        listOf(),
                        listOf(
                            SDMXExchangeRate(
                                SDMXExchangeRateDate(exchangeRateDate),
                                SDMXExchangeRateValue((it.second)),
                            ),
                        ),
                    )
                },
            ),
        )
}
