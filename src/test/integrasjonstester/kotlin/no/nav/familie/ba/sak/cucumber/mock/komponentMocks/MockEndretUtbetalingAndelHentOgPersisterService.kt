package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.cucumber.VedtaksperioderOgBegrunnelserStepDefinition
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelHentOgPersisterService

fun mockEndretUtbetalingAndelHentOgPersisterService(dataFraCucumber: VedtaksperioderOgBegrunnelserStepDefinition): EndretUtbetalingAndelHentOgPersisterService {
    val endretUtbetalingAndelHentOgPersisterService = mockk<EndretUtbetalingAndelHentOgPersisterService>()
    every { endretUtbetalingAndelHentOgPersisterService.hentForBehandling(any()) } answers {
        val behandlingId = firstArg<Long>()
        dataFraCucumber.endredeUtbetalinger[behandlingId] ?: emptyList()
    }
    return endretUtbetalingAndelHentOgPersisterService
}
