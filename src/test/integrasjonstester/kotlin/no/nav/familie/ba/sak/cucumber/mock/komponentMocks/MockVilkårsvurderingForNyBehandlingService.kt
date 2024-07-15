package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.cucumber.VedtaksperioderOgBegrunnelserStepDefinition
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.VilkårsvurderingForNyBehandlingService

fun mockVilkårsvurderingForNyBehandlingService(dataFraCucumber: VedtaksperioderOgBegrunnelserStepDefinition): VilkårsvurderingForNyBehandlingService {
    val vilkårsvurderingForNyBehandlingService = mockk<VilkårsvurderingForNyBehandlingService>()
    every { vilkårsvurderingForNyBehandlingService.opprettVilkårsvurderingUtenomHovedflyt(any(), any(), any()) } answers {
        val behandling = firstArg<Behandling>()
        val forrigeBehandling = secondArg<Behandling>()

        val forrigeVilkårsvurdering = dataFraCucumber.vilkårsvurderinger[forrigeBehandling.id]!!
        val vilkårsvurderingSmåbarnstilleggbehandling = forrigeVilkårsvurdering.copy(behandling = behandling).kopier()

        dataFraCucumber.vilkårsvurderinger[behandling.id] = vilkårsvurderingSmåbarnstilleggbehandling
    }
    return vilkårsvurderingForNyBehandlingService
}
