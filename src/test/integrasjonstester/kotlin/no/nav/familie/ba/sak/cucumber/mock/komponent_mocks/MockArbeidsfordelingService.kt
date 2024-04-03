package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService

fun mockArbeidsfordelingService(): ArbeidsfordelingService {
    val arbeidsfordelingService = mockk<ArbeidsfordelingService>()
    every { arbeidsfordelingService.fastsettBehandlendeEnhet(any(), any()) } just runs
    return arbeidsfordelingService
}
