package no.nav.familie.ba.sak.cucumber.mock.komponentMocks

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.integrasjoner.økonomi.OppdragBackendKlient
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus

fun mockOppdragBackendKlient(): OppdragBackendKlient {
    val mockOppdragBackendKlient = mockk<OppdragBackendKlient>()
    every { mockOppdragBackendKlient.iverksettOppdrag(any()) } returns ""
    every { mockOppdragBackendKlient.hentStatus(any()) } returns OppdragStatus.KVITTERT_OK
    return mockOppdragBackendKlient
}
