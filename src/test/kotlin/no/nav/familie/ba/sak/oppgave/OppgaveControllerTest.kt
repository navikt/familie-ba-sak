package no.nav.familie.ba.sak.oppgave

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.Tema
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

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
        Assertions.assertThat(oppgaver).hasSize(1)
        Assertions.assertThat(oppgaver.first().tema).isEqualTo(Tema.BAR)
    }

    @Test
    fun `finnOppgaverKnyttetTilSaksbehandlerOgEnhet skal feile ved ukjent behandlingstema`() {
        val oppgaver = oppgaveController.finnOppgaverKnyttetTilSaksbehandlerOgEnhet("ab1000", null, null, null)
        Assertions.assertThat(oppgaver.body?.status).isEqualTo(Ressurs.Status.FEILET)
        Assertions.assertThat(oppgaver.body?.melding).isEqualTo("Ugyldig behandlingstema")
    }
}