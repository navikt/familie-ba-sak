package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.familie.ba.sak.kjerne.eøs.endringsabonnement.TilpassKompetanserTilRegelverkService

fun mockTilpassKompetanserTilRegelverkService(): TilpassKompetanserTilRegelverkService {
    val tilpassKompetanserTilRegelverkService = mockk<TilpassKompetanserTilRegelverkService>()
    every { tilpassKompetanserTilRegelverkService.tilpassKompetanserTilRegelverk(any()) } just runs
    return tilpassKompetanserTilRegelverkService
}
