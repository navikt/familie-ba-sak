package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.cucumber.VedtaksperioderOgBegrunnelserStepDefinition
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.PersonopplysningGrunnlagForNyBehandlingService

fun mockPersonopplysningGrunnlagForNyBehandlingService(dataFraCucumber: VedtaksperioderOgBegrunnelserStepDefinition): PersonopplysningGrunnlagForNyBehandlingService {
    val personopplysningGrunnlagForNyBehandlingService = mockk<PersonopplysningGrunnlagForNyBehandlingService>()
    every { personopplysningGrunnlagForNyBehandlingService.opprettKopiEllerNyttPersonopplysningGrunnlag(any(), any(), any(), any()) } answers {
        val behandling = firstArg<Behandling>()
        val forrigeBehandling = secondArg<Behandling>()

        val persongrunnlagForrigeBehandling = dataFraCucumber.persongrunnlag[forrigeBehandling.id]!!
        dataFraCucumber.persongrunnlag[behandling.id] = persongrunnlagForrigeBehandling.copy(behandlingId = behandling.id)
    }
    return personopplysningGrunnlagForNyBehandlingService
}
