package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService

fun mockOppgaveService(): OppgaveService {
    val oppgaveService = mockk<OppgaveService>()
    every { oppgaveService.opprettOppgaveForManuellBehandling(any(), any(), any(), any()) } returns ""
    return oppgaveService
}
