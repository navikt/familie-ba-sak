package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.familie.ba.sak.sikkerhet.TilgangService

fun mockTilgangService(): TilgangService {
    val tilgangService = mockk<TilgangService>()
    every { tilgangService.validerTilgangTilBehandling(any(), any()) } just runs
    every { tilgangService.verifiserHarTilgangTilHandling(any(), any()) } just runs
    return tilgangService
}
