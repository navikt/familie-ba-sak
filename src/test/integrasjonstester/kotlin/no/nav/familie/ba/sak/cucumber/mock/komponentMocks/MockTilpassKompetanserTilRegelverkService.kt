package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.familie.ba.sak.kjerne.eøs.endringsabonnement.TilpassKompetanserTilEndretUtbetalingAndelerService

fun mockTilpassKompetanserTilEndretUtebetalingAndelerService(): TilpassKompetanserTilEndretUtbetalingAndelerService {
    val tilpassKompetanserTilEndretUtbetalingAndelerService = mockk<TilpassKompetanserTilEndretUtbetalingAndelerService>()
    every { tilpassKompetanserTilEndretUtbetalingAndelerService.tilpassKompetanserTilEndretUtbetalingAndeler(any(), any()) } just runs
    return tilpassKompetanserTilEndretUtbetalingAndelerService
}
