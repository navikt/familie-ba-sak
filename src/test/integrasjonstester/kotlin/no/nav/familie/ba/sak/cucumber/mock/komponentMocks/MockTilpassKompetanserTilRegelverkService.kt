package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.familie.ba.sak.kjerne.eøs.endringsabonnement.TilpassKompetanserTilEndretUtebetalingAndelerService

fun mockTilpassKompetanserTilEndretUtebetalingAndelerService(): TilpassKompetanserTilEndretUtebetalingAndelerService {
    val tilpassKompetanserTilEndretUtebetalingAndelerService = mockk<TilpassKompetanserTilEndretUtebetalingAndelerService>()
    every { tilpassKompetanserTilEndretUtebetalingAndelerService.tilpassKompetanserTilEndretUtbetalingAndeler(any(), any()) } just runs
    return tilpassKompetanserTilEndretUtebetalingAndelerService
}
