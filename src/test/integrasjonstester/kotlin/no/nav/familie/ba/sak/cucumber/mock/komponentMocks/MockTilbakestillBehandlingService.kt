package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingService

fun mockTilbakestillBehandlingService(): TilbakestillBehandlingService {
    val tilbakestillBehandlingService = mockk<TilbakestillBehandlingService>()
    every { tilbakestillBehandlingService.tilbakestillDataTilVilkårsvurderingssteg(any()) } just runs
    return tilbakestillBehandlingService
}
