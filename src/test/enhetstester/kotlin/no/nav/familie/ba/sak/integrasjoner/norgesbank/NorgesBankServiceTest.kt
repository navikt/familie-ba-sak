package no.nav.familie.ba.sak.integrasjoner.norgesbank

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.integrasjoner.ecb.domene.ECBValutakursCache
import no.nav.familie.ba.sak.integrasjoner.ecb.domene.ECBValutakursCacheRepository
import no.nav.familie.valutakurs.NorgesBankValutakursRestKlient
import no.nav.familie.valutakurs.domene.Valutakurs
import no.nav.familie.valutakurs.domene.norgesbank.Frekvens
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class NorgesBankServiceTest {
    private val norgesBankValutakursRestKlient = mockk<NorgesBankValutakursRestKlient>()
    private val ecbValutakursCacheRepository = mockk<ECBValutakursCacheRepository>()

    private val norgesBankService = NorgesBankService(norgesBankValutakursRestKlient, ecbValutakursCacheRepository)

    @Test
    fun `hentValutakurs skal hente valutakurs fra Norges Bank og lagre den i cache dersom den ikke finnes i cache fra før`() {
        // Arrange
        val utenlandskValuta = "SEK"
        val kursDato = LocalDate.of(2023, 5, 15)
        val kurs = BigDecimal.valueOf(0.95)

        every { ecbValutakursCacheRepository.findByValutakodeAndValutakursdato(utenlandskValuta, kursDato) } returns emptyList()
        every {
            norgesBankValutakursRestKlient.hentValutakurs(Frekvens.VIRKEDAG, utenlandskValuta, kursDato)
        } returns Valutakurs(valuta = utenlandskValuta, kurs = kurs, kursDato = kursDato)
        every { ecbValutakursCacheRepository.save(any()) } answers { firstArg() }

        // Act
        val hentetValutakurs = norgesBankService.hentValutakurs(utenlandskValuta, kursDato)

        // Assert
        assertEquals(kurs, hentetValutakurs)
        verify(exactly = 1) { norgesBankValutakursRestKlient.hentValutakurs(Frekvens.VIRKEDAG, utenlandskValuta, kursDato) }
        verify(exactly = 1) {
            ecbValutakursCacheRepository.save(
                ECBValutakursCache(
                    kurs = kurs,
                    valutakode = utenlandskValuta,
                    valutakursdato = kursDato,
                ),
            )
        }
    }

    @Test
    fun `hentValutakurs skal returnere valutakurs fra cache dersom den finnes fra før uten å kalle Norges Bank`() {
        // Arrange
        val utenlandskValuta = "SEK"
        val kursDato = LocalDate.of(2023, 5, 15)
        val kurs = BigDecimal.valueOf(0.95)
        val cachetValutakurs =
            ECBValutakursCache(
                kurs = kurs,
                valutakode = utenlandskValuta,
                valutakursdato = kursDato,
            )

        every {
            ecbValutakursCacheRepository.findByValutakodeAndValutakursdato(utenlandskValuta, kursDato)
        } returns listOf(cachetValutakurs)

        // Act
        val hentetValutakurs = norgesBankService.hentValutakurs(utenlandskValuta, kursDato)

        // Assert
        assertEquals(kurs, hentetValutakurs)
        verify(exactly = 0) { norgesBankValutakursRestKlient.hentValutakurs(any(), any(), any()) }
        verify(exactly = 0) { ecbValutakursCacheRepository.save(any()) }
    }
}
