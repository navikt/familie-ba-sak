package no.nav.familie.ba.sak.kjerne.automatiskVurdering

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonInfoQuery
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class pdlMockTest {

    val mockPdlRestClient: PdlRestClient = mockk(relaxed = true)
    
    @Test
    fun `Skal returnere barn og søker`() {
        every { mockPdlRestClient.hentPerson("21111777001", any()) } returns mockBarnAutomatiskBehandling
        every { mockPdlRestClient.hentPerson("04086226621", any()) } returns mockSøkerAutomatiskBehandling

        val barn = mockPdlRestClient.hentPerson("21111777001", PersonInfoQuery.MED_RELASJONER)
        val søker = mockPdlRestClient.hentPerson("04086226621", PersonInfoQuery.MED_RELASJONER)

        Assertions.assertEquals("ARTIG MIDTPUNKT", barn.navn)
        Assertions.assertEquals("LEALAUS GYNGEHEST", søker.navn)
    }
}