package no.nav.familie.ba.sak.integrasjoner.ecb

import no.nav.familie.ba.sak.config.restTemplate
import no.nav.familie.ba.sak.integrasjoner.ecb.domene.exchangeRatesForCurrency
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ECBClientTest {

    @Test
    fun testHentValutakurs() {
        val ecbClient = ECBClient(restTemplate, "https://sdw-wsrest.ecb.europa.eu/service/data/EXR/")
        val valutakursDato = LocalDate.of(2022, 7, 22)
        val ecbValutakursData = ecbClient.getECBExchangeRatesData("SEK", valutakursDato)
        assertNotNull(ecbValutakursData)
        assertEquals(2, ecbValutakursData.exchangeRatesForCountries.size)
        val sekValutakurs = ecbValutakursData.exchangeRatesForCurrency("SEK")
        val nokValutakurs = ecbValutakursData.exchangeRatesForCurrency("NOK")
        assertEquals(1, sekValutakurs.size)
        assertEquals(1, nokValutakurs.size)
        assertEquals(valutakursDato.toString(), sekValutakurs[0].date)
        assertEquals(valutakursDato.toString(), nokValutakurs[0].date)
    }
}
