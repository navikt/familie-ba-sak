package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.kjerne.tilbakekreving.TilbakekrevingService

fun mockTilbakekrevingService(): TilbakekrevingService {
    val tilbakekrevingService = mockk<TilbakekrevingService>()
    every { tilbakekrevingService.slettTilbakekrevingPåBehandling(any()) } returns null
    return tilbakekrevingService
}
