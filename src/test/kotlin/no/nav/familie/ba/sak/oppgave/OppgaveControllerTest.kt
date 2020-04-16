package no.nav.familie.ba.sak.oppgave

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.familie.ba.sak.oppgave.domene.OppgaveDto
import no.nav.familie.kontrakter.felles.Ressurs
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class OppgaveControllerTest {

    @MockK
    lateinit var oppgaveService: OppgaveService

    @InjectMockKs
    lateinit var oppgaveController: OppgaveController

    @Test
    fun `finnOppgaverKnyttetTilSaksbehandlerOgEnhet via OppgaveController skal fungere`() {
        every {
            oppgaveService.finnOppgaverKnyttetTilSaksbehandlerOgEnhet(any(), any(), any(), any())
        } returns listOf(OppgaveDto(tema = "BAR"))
        val response = oppgaveController.finnOppgaverKnyttetTilSaksbehandlerOgEnhet(null, null, null, null)
        val oppgaver = response.body?.data as List<OppgaveDto>
        Assertions.assertThat(oppgaver).hasSize(1)
        Assertions.assertThat(oppgaver.first().tema).isEqualTo("BAR")
    }

    @Test
    fun `finnOppgaverKnyttetTilSaksbehandlerOgEnhet skal feile ved ukjent behandlingstema`() {
        val oppgaver = oppgaveController.finnOppgaverKnyttetTilSaksbehandlerOgEnhet("ab1000", null, null, null)
        Assertions.assertThat(oppgaver.body?.status).isEqualTo(Ressurs.Status.FEILET)
        Assertions.assertThat(oppgaver.body?.melding).isEqualTo("Ugyldig behandlingstema")
    }
}