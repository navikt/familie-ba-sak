package no.nav.familie.ba.sak.oppgave

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.kontrakter.felles.Ressurs
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

    @InjectMockKs
    lateinit var oppgaveController: OppgaveController

    // Trengs for autowiring av oppgave service
    @MockK
    lateinit var integrasjonClient: IntegrasjonClient

    @Test
    fun `finnOppgaverKnyttetTilSaksbehandlerOgEnhet via OppgaveController skal fungere`() {
        every {
            oppgaveService.finnOppgaverKnyttetTilSaksbehandlerOgEnhet(any(), any(), any(), any())
        } returns listOf(Oppgave(tema = Tema.BAR))
        val response = oppgaveController.finnOppgaverKnyttetTilSaksbehandlerOgEnhet(null, null, null, null)
        val oppgaver = response.body?.data as List<Oppgave>
        Assertions.assertEquals(1, oppgaver.size)
        Assertions.assertEquals(Tema.BAR, oppgaver.first().tema)
    }

    @Test
    fun `finnOppgaverKnyttetTilSaksbehandlerOgEnhet skal feile ved ukjent behandlingstema`() {
        val oppgaver = oppgaveController.finnOppgaverKnyttetTilSaksbehandlerOgEnhet("ab1000", null, null, null)
        Assertions.assertEquals(Ressurs.Status.FEILET, oppgaver.body?.status)
        Assertions.assertEquals("Ugyldig behandlingstema", oppgaver.body?.melding)
    }

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
    fun `Tilbakestilling av tildeling på oppgave skal returnere OK og sende med OppgaveId i respons`() {
        val OPPGAVE_ID = "1234"
        every { oppgaveService.tilbakestillFordelingPåOppgave(any()) } returns OPPGAVE_ID

        val respons = oppgaveController.tilbakestillFordelingPåOppgave(OPPGAVE_ID.toLong())

        Assertions.assertEquals(HttpStatus.OK, respons.statusCode)
        Assertions.assertEquals(OPPGAVE_ID, respons.body?.data)
    }
}