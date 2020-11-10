package no.nav.familie.ba.sak.oppgave

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonException
import no.nav.familie.ba.sak.oppgave.domene.RestFinnOppgaveRequest
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.Tema
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus

@ExtendWith(MockKExtension::class)
class OppgaveControllerTest {

    @MockK
    lateinit var oppgaveService: OppgaveService

    @MockK
    lateinit var personopplysningerService: PersonopplysningerService

    @MockK
    lateinit var integrasjonClient: IntegrasjonClient

    @MockK
    lateinit var fagsakService: FagsakService

    @InjectMockKs
    lateinit var oppgaveController: OppgaveController

    @Test
    fun `Tildeling av oppgave til saksbehandler skal returnere OK og sende med OppgaveId i respons`() {
        val OPPGAVE_ID = "1234"
        val SAKSBEHANDLER_ID = "Z999999"
        every { oppgaveService.fordelOppgave(any(), any()) } returns OPPGAVE_ID

        val respons = oppgaveController.fordelOppgave(OPPGAVE_ID.toLong(), SAKSBEHANDLER_ID)

        Assertions.assertEquals(HttpStatus.OK, respons.statusCode)
        Assertions.assertEquals(OPPGAVE_ID, respons.body?.data)
    }

    @Test
    fun `Tilbakestilling av tildeling på oppgave skal returnere OK og sende med Oppgave i respons`() {
        val oppgave = Oppgave(
                id = 1234,
        )
        every { oppgaveService.tilbakestillFordelingPåOppgave(oppgave.id!!) } returns oppgave

        val respons = oppgaveController.tilbakestillFordelingPåOppgave(oppgave.id!!)

        Assertions.assertEquals(HttpStatus.OK, respons.statusCode)
        Assertions.assertEquals(oppgave, respons.body?.data)
    }

    @Test
    fun `Tildeling av oppgave skal returnere feil ved feil fra integrasjonsklienten`() {
        val OPPGAVE_ID = "1234"
        val SAKSBEHANDLER_ID = "Z999998"
        every {
            oppgaveService.fordelOppgave(any(),
                                         any())
        } throws IntegrasjonException("Kall mot integrasjon feilet ved fordel oppgave")

        val respons = oppgaveController.fordelOppgave(OPPGAVE_ID.toLong(), SAKSBEHANDLER_ID)

        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, respons.statusCode)
        Assertions.assertEquals("Feil ved tildeling av oppgave", respons.body?.melding)
    }

    @Test
    fun `hentOppgaver via OppgaveController skal fungere`() {
        every {
            oppgaveService.hentOppgaver(any())
        } returns FinnOppgaveResponseDto(1, listOf(Oppgave(tema = Tema.BAR)))
        val response = oppgaveController.hentOppgaver(RestFinnOppgaveRequest())
        val oppgaverOgAntall = response.body?.data as FinnOppgaveResponseDto
        Assertions.assertEquals(1, oppgaverOgAntall.antallTreffTotalt)
        Assertions.assertEquals(Tema.BAR, oppgaverOgAntall.oppgaver.first().tema)
    }
}