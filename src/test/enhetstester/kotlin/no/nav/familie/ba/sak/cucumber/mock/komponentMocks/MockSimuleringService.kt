package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import java.math.BigDecimal

fun mockSimuleringService(): SimuleringService {
    val simuleringService = mockk<SimuleringService>()
    every { simuleringService.oppdaterSimuleringPåBehandling(any()) } returns emptyList()
    every { simuleringService.hentSimuleringPåBehandling(any()) } returns emptyList()
    every { simuleringService.oppdaterSimuleringPåBehandlingVedBehov(any()) } returns emptyList()
    every { simuleringService.hentEtterbetaling(any<Long>()) } returns BigDecimal.ZERO
    every { simuleringService.hentFeilutbetaling(any<Long>()) } returns BigDecimal.ZERO
    return simuleringService
}
