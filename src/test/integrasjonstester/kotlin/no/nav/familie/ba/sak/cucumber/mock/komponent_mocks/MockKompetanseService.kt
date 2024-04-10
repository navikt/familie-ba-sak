package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.cucumber.BegrunnelseTeksterStepDefinition
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseService

fun mockKompetanseService(dataFraCucumber: BegrunnelseTeksterStepDefinition): KompetanseService {
    val kompetanseService = mockk<KompetanseService>()
    every { kompetanseService.hentKompetanser(any()) } answers {
        val behandlingId = firstArg<BehandlingId>()
        dataFraCucumber.kompetanser[behandlingId.id] ?: emptyList()
    }
    return kompetanseService
}
