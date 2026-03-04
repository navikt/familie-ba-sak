package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.cucumber.VedtaksperioderOgBegrunnelserStepDefinition
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository

fun mockEndretUtbetalingAndelRepository(dataFraCucumber: VedtaksperioderOgBegrunnelserStepDefinition): EndretUtbetalingAndelRepository {
    val endretUtbetalingAndelRepository = mockk<EndretUtbetalingAndelRepository>()
    every { endretUtbetalingAndelRepository.findByBehandlingId(any()) } answers {
        val behandlingId = firstArg<Long>()
        dataFraCucumber.endredeUtbetalinger[behandlingId] ?: emptyList()
    }
    every { endretUtbetalingAndelRepository.save(any()) } answers {
        val endretUtbetalingAndel = firstArg<EndretUtbetalingAndel>()
        val behandlingId = endretUtbetalingAndel.behandlingId
        val eksisterendeAndeler = dataFraCucumber.endredeUtbetalinger[behandlingId].orEmpty()
        dataFraCucumber.endredeUtbetalinger[behandlingId] = eksisterendeAndeler + endretUtbetalingAndel
        endretUtbetalingAndel
    }
    return endretUtbetalingAndelRepository
}
