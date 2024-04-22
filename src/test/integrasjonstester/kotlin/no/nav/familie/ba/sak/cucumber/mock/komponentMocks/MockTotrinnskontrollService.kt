package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.familie.ba.sak.cucumber.BegrunnelseTeksterStepDefinition
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.domene.Totrinnskontroll
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext

fun mockTotrinnskontrollService(dataFraCucumber: BegrunnelseTeksterStepDefinition): TotrinnskontrollService {
    val totrinnskontrollService = mockk<TotrinnskontrollService>()
    every { totrinnskontrollService.opprettAutomatiskTotrinnskontroll(any()) } just runs
    every { totrinnskontrollService.hentAktivForBehandling(any()) } answers {
        val behandlingId = firstArg<Long>()
        Totrinnskontroll(
            behandling = dataFraCucumber.behandlinger[behandlingId]!!,
            godkjent = true,
            saksbehandler = SikkerhetContext.SYSTEM_NAVN,
            saksbehandlerId = SikkerhetContext.SYSTEM_FORKORTELSE,
            beslutter = SikkerhetContext.SYSTEM_NAVN,
            beslutterId = SikkerhetContext.SYSTEM_FORKORTELSE,
        )
    }
    return totrinnskontrollService
}
