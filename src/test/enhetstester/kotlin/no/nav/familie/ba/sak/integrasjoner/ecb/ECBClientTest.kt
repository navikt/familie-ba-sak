package no.nav.familie.ba.sak.integrasjoner.ecb

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.integrasjoner.ecb.domene.exchangeRatesForCurrency
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class ECBClientTest(
    @Autowired
    val ecbClient: ECBClient
) : AbstractSpringIntegrationTest() {

    @Test
    fun `Test at ECBClient henter kurser for både SEK og NOK og at valutakursdatoen er korrekt`() {
        val valutakursDato = LocalDate.of(2022, 7, 22)
        val valutakurser = ecbClient.getExchangeRates("SEK", valutakursDato)
        assertNotNull(valutakurser)
        assertEquals(2, valutakurser.ecbExchangeRatesDataSet.ecbExchangeRatesForCurrencies.size)
        val sekValutakurs = valutakurser.exchangeRatesForCurrency("SEK")
        val nokValutakurs = valutakurser.exchangeRatesForCurrency("NOK")
        assertEquals(1, sekValutakurs.size)
        assertEquals(1, nokValutakurs.size)
        assertEquals(valutakursDato.toString(), sekValutakurs.first().date.value)
        assertEquals(valutakursDato.toString(), nokValutakurs.first().date.value)
    }

    @Test
    fun `Test at ECBClient henter kurs kun for NOK dersom utenlandskValuta er EUR`() {
        val valutakursDato = LocalDate.of(2022, 7, 22)
        val valutakurser = ecbClient.getExchangeRates("EUR", valutakursDato)
        assertNotNull(valutakurser)
        assertEquals(1, valutakurser.ecbExchangeRatesDataSet.ecbExchangeRatesForCurrencies.size)
        val eurValutakurs = valutakurser.exchangeRatesForCurrency("EUR")
        val nokValutakurs = valutakurser.exchangeRatesForCurrency("NOK")
        assertEquals(0, eurValutakurs.size)
        assertEquals(1, nokValutakurs.size)
        assertEquals(valutakursDato.toString(), nokValutakurs.first().date.value)
    }
}
