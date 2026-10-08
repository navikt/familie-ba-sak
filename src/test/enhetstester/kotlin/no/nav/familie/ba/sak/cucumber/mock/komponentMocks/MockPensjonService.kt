package no.nav.familie.ba.sak.cucumber.mock.komponentMocks

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.familie.ba.sak.ekstern.pensjon.PensjonService

fun mockPensjonService(): PensjonService {
    val pensjonService = mockk<PensjonService>()
    every { pensjonService.opprettTaskForSendingAvMeldingOmFerdigstiltBehandling(any()) } just runs
    return pensjonService
}
