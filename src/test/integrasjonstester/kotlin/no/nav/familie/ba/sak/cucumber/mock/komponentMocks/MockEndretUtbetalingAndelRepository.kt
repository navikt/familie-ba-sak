package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.cucumber.VedtaksperioderOgBegrunnelserStepDefinition
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository

fun mockEndretUtbetalingAndelRepository(dataFraCucumber: VedtaksperioderOgBegrunnelserStepDefinition): EndretUtbetalingAndelRepository {
    val endretUtbetalingAndelRepository = mockk<EndretUtbetalingAndelRepository>()
    every { endretUtbetalingAndelRepository.findByBehandlingId(any()) } answers {
        val behandlingId = firstArg<Long>()
        dataFraCucumber.endredeUtbetalinger[behandlingId] ?: emptyList()
    }
    return endretUtbetalingAndelRepository
}
