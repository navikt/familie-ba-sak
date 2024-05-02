package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.EøsSkjemaerForNyBehandlingService

fun mockEøsSkjemaerForNyBehandlingService(): EøsSkjemaerForNyBehandlingService {
    val eøsSkjemaerForNyBehandlingService = mockk<EøsSkjemaerForNyBehandlingService>()
    every { eøsSkjemaerForNyBehandlingService.kopierEøsSkjemaer(any(), any()) } just runs
    return eøsSkjemaerForNyBehandlingService
}
