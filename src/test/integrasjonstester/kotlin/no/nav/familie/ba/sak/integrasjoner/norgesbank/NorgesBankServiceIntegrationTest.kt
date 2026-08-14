package no.nav.familie.ba.sak.integrasjoner.norgesbank

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.integrasjoner.ecb.domene.ECBValutakursCacheRepository
import no.nav.familie.valutakurs.NorgesBankValutakursRestKlient
import no.nav.familie.valutakurs.domene.Valutakurs
import no.nav.familie.valutakurs.domene.norgesbank.Frekvens
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate

class NorgesBankServiceIntegrationTest(
    @Autowired
    private val ecbValutakursCacheRepository: ECBValutakursCacheRepository,
) : AbstractSpringIntegrationTest() {
    private val norgesBankValutakursRestKlient = mockk<NorgesBankValutakursRestKlient>()

    private val norgesBankService =
        NorgesBankService(
            norgesBankValutakursRestKlient = norgesBankValutakursRestKlient,
            ecbValutakursCacheRepository = ecbValutakursCacheRepository,
        )

    @Test
    fun `Skal teste at valutakurs hentes fra cache dersom valutakursen allerede er hentet fra NorgesBank`() {
        // Arrange
        val kursDato = LocalDate.of(2026, 7, 31)
        val valuta = "SEK"
        val kurs = BigDecimal.valueOf(0.9960)
        every {
            norgesBankValutakursRestKlient.hentValutakurs(
                Frekvens.VIRKEDAG,
                valuta,
                kursDato,
            )
        } returns
            Valutakurs(
                valuta = valuta,
                kurs = kurs,
                kursDato = kursDato,
            )

        // Act & Assert
        norgesBankService.hentValutakurs(valuta, kursDato)
        val valutakurs = ecbValutakursCacheRepository.findByValutakodeAndValutakursdato(valuta, kursDato)?.firstOrNull()
        assertThat(valutakurs!!.kurs).isEqualTo(kurs)
        assertThat(valutakurs.valutakode).isEqualTo(valuta)
        assertThat(valutakurs.valutakursdato).isEqualTo(kursDato)

        norgesBankService.hentValutakurs(valuta, kursDato)
        verify(exactly = 1) {
            norgesBankValutakursRestKlient.hentValutakurs(
                any(),
                any(),
                any(),
            )
        }
    }
}
