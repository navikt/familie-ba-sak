package no.nav.familie.ba.sak.integrasjoner.ecb

import no.nav.familie.ba.sak.config.restTemplate
import no.nav.familie.ba.sak.integrasjoner.ecb.domene.exchangeRatesForCurrency
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ECBClientTest {

    val ecbApiUrl = "https://sdw-wsrest.ecb.europa.eu/service/data/EXR/"
    lateinit var ecbClient: ECBClient

    @BeforeAll
    fun setup() {
        ecbClient = ECBClient(restTemplate, ecbApiUrl)
    }

    @Test
    fun `Test at ECBClient henter kurser for både SEK og NOK og at valutakursdatoen er korrekt`() {
        val valutakursDato = LocalDate.of(2022, 7, 22)
        val valutakurser = ecbClient.getExchangeRates("SEK", valutakursDato)
        assertNotNull(valutakurser)
        assertEquals(2, valutakurser.exchangeRatesForCurrencies.size)
        val sekValutakurs = valutakurser.exchangeRatesForCurrency("SEK")
        val nokValutakurs = valutakurser.exchangeRatesForCurrency("NOK")
        assertEquals(1, sekValutakurs.size)
        assertEquals(1, nokValutakurs.size)
        assertEquals(valutakursDato.toString(), sekValutakurs.first().date)
        assertEquals(valutakursDato.toString(), nokValutakurs.first().date)
    }

    @Test
    fun `Test at ECBClient henter kurs kun for NOK dersom utenlandskValuta er EUR`() {
        val valutakursDato = LocalDate.of(2022, 7, 22)
        val valutakurser = ecbClient.getExchangeRates("EUR", valutakursDato)
        assertNotNull(valutakurser)
        assertEquals(1, valutakurser.exchangeRatesForCurrencies.size)
        val eurValutakurs = valutakurser.exchangeRatesForCurrency("EUR")
        val nokValutakurs = valutakurser.exchangeRatesForCurrency("NOK")
        assertEquals(0, eurValutakurs.size)
        assertEquals(1, nokValutakurs.size)
        assertEquals(valutakursDato.toString(), nokValutakurs.first().date)
    }
}
