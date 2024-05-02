package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService

fun mockSimuleringService(): SimuleringService {
    val simuleringService = mockk<SimuleringService>()
    every { simuleringService.oppdaterSimuleringPåBehandling(any()) } returns emptyList()
    every { simuleringService.hentSimuleringPåBehandling(any()) } returns emptyList()
    return simuleringService
}
