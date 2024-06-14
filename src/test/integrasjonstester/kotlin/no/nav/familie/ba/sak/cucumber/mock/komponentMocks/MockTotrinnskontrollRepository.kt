package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.cucumber.BegrunnelseTeksterStepDefinition
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollRepository
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.domene.Totrinnskontroll

fun mockTotrinnskontrollRepository(dataFraCucumber: BegrunnelseTeksterStepDefinition): TotrinnskontrollRepository {
    val totrinnskontrollRepository = mockk<TotrinnskontrollRepository>()

    every { totrinnskontrollRepository.save(any()) } answers {
        val nyTotrinnskontroll = firstArg<Totrinnskontroll>()
        dataFraCucumber.totrinnskontroller[nyTotrinnskontroll.behandling.id] = nyTotrinnskontroll
        nyTotrinnskontroll
    }

    every { totrinnskontrollRepository.findByBehandlingAndAktiv(any()) } answers {
        val behandlingId = firstArg<Long>()
        dataFraCucumber.totrinnskontroller[behandlingId]
    }

    return totrinnskontrollRepository
}
