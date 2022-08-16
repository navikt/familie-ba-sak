package no.nav.familie.ba.sak.integrasjoner.ecb

import no.nav.familie.ba.sak.config.restTemplate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class ECBServiceTest {

    @Test
    fun `Hent valutakurs for utenlandsk valuta til NOK og sjekk at beregning av kurs er riktig`() {
        val ecbService = ECBService(ECBClient(restTemplate, "https://sdw-wsrest.ecb.europa.eu/service/data/EXR/"))
        val valutakursDato = LocalDate.of(2022, 6, 28)
        val SEKtilNOKValutakurs = ecbService.hentValutakurs("SEK", valutakursDato)
        assertEquals(BigDecimal.valueOf(0.9702), SEKtilNOKValutakurs)
    }
}
