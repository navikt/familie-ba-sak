package no.nav.familie.ba.sak.integrasjoner.ecb

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.integrasjoner.ecb.domene.ECBValutakursCacheRepository
import no.nav.familie.valutakurs.Frequency
import no.nav.familie.valutakurs.ValutakursRestClient
import no.nav.familie.valutakurs.domene.ECBExchangeRate
import no.nav.familie.valutakurs.domene.ECBExchangeRateDate
import no.nav.familie.valutakurs.domene.ECBExchangeRateKey
import no.nav.familie.valutakurs.domene.ECBExchangeRateValue
import no.nav.familie.valutakurs.domene.ECBExchangeRatesData
import no.nav.familie.valutakurs.domene.ECBExchangeRatesDataSet
import no.nav.familie.valutakurs.domene.ECBExchangeRatesForCurrency
import no.nav.familie.valutakurs.domene.toExchangeRates
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ECBIntegrationTest : AbstractSpringIntegrationTest() {
    @MockK
    private lateinit var ecbClient: ValutakursRestClient

    @Autowired
    private lateinit var ecbService: ECBService

    @Autowired
    private lateinit var ecbValutakursCacheRepository: ECBValutakursCacheRepository

    @Autowired
    private lateinit var databaseCleanupService: DatabaseCleanupService

    @BeforeEach
    fun setUp() {
        ecbService =
            ECBService(
                ecbClient = ecbClient,
                ecbValutakursCacheRepository = ecbValutakursCacheRepository,
            )
        databaseCleanupService.truncate()
    }

    @Test
    fun `Skal teste at valutakurs hentes fra cache dersom valutakursen allerede er hentet fra ECB`() {
        val valutakursDato = LocalDate.of(2022, 7, 20)
        val ecbExchangeRatesData =
            createECBResponse(
                Frequency.Daily,
                listOf(Pair("NOK", BigDecimal.valueOf(9.4567))),
                valutakursDato.toString(),
            )
        every {
            ecbClient.hentValutakurs(
                any(),
                any(),
                any(),
            )
        } returns ecbExchangeRatesData.toExchangeRates()

        ecbService.hentValutakurs("EUR", valutakursDato)
        val valutakurs = ecbValutakursCacheRepository.findByValutakodeAndValutakursdato("EUR", valutakursDato)?.firstOrNull()
        assertEquals(valutakurs!!.kurs, BigDecimal.valueOf(9.4567))
        ecbService.hentValutakurs("EUR", valutakursDato)
        verify(exactly = 1) {
            ecbClient.hentValutakurs(
                any(),
                any(),
                any(),
            )
        }
    }

    private fun createECBResponse(
        frequency: Frequency,
        exchangeRates: List<Pair<String, BigDecimal>>,
        exchangeRateDate: String,
    ): ECBExchangeRatesData =
        ECBExchangeRatesData(
            ECBExchangeRatesDataSet(
                exchangeRates.map {
                    ECBExchangeRatesForCurrency(
                        listOf(
                            ECBExchangeRateKey("CURRENCY", it.first),
                            ECBExchangeRateKey("FREQ", frequency.toFrequencyParam()),
                        ),
                        listOf(
                            ECBExchangeRate(
                                ECBExchangeRateDate(exchangeRateDate),
                                ECBExchangeRateValue((it.second)),
                            ),
                        ),
                    )
                },
            ),
        )
}
