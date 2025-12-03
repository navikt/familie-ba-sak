package no.nav.familie.ba.sak.integrasjoner.ecb

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.integrasjoner.ecb.domene.ECBValutakursCacheRepository
import no.nav.familie.valutakurs.ECBValutakursRestKlient
import no.nav.familie.valutakurs.NorgesBankValutakursRestKlient
import no.nav.familie.valutakurs.domene.ecb.ECBValutakursData
import no.nav.familie.valutakurs.domene.ecb.Frequency
import no.nav.familie.valutakurs.domene.ecb.toExchangeRates
import no.nav.familie.valutakurs.domene.sdmx.SDMXExchangeRate
import no.nav.familie.valutakurs.domene.sdmx.SDMXExchangeRateDate
import no.nav.familie.valutakurs.domene.sdmx.SDMXExchangeRateKey
import no.nav.familie.valutakurs.domene.sdmx.SDMXExchangeRateValue
import no.nav.familie.valutakurs.domene.sdmx.SDMXExchangeRatesDataSet
import no.nav.familie.valutakurs.domene.sdmx.SDMXExchangeRatesForCurrency
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate

class ECBServiceIntegrationTest(
    @Autowired private val ecbValutakursCacheRepository: ECBValutakursCacheRepository,
) : AbstractSpringIntegrationTest() {
    private val ecbValutakursRestKlient = mockk<ECBValutakursRestKlient>()
    private val norgesBankValutakursRestKlient = mockk<NorgesBankValutakursRestKlient>(relaxed = true)

    private val ecbService = ECBService(ecbValutakursRestKlient = ecbValutakursRestKlient, norgesBankValutakursRestKlient, ecbValutakursCacheRepository = ecbValutakursCacheRepository)

    @Test
    fun `Skal teste at valutakurs hentes fra cache dersom valutakursen allerede er hentet fra ECB`() {
        // Arrange
        val valutakursDato = LocalDate.of(2022, 7, 20)
        val ecbExchangeRatesData =
            createECBResponse(
                Frequency.Daily,
                listOf(Pair("NOK", BigDecimal.valueOf(9.4567))),
                valutakursDato.toString(),
            )
        every {
            ecbValutakursRestKlient.hentValutakurs(
                Frequency.Daily,
                listOf("NOK", "EUR"),
                valutakursDato,
            )
        } returns ecbExchangeRatesData.toExchangeRates()

        // Act
        ecbService.hentValutakurs("EUR", valutakursDato)
        val valutakurs = ecbValutakursCacheRepository.findByValutakodeAndValutakursdato("EUR", valutakursDato)?.firstOrNull()

        // Assert
        assertEquals(valutakurs!!.kurs, BigDecimal.valueOf(9.4567))

        ecbService.hentValutakurs("EUR", valutakursDato)
        verify(exactly = 1) {
            ecbValutakursRestKlient.hentValutakurs(
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
