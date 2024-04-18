package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.cucumber.BegrunnelseTeksterStepDefinition
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService

fun mockVilkårsvurderingService(dataFraCucumber: BegrunnelseTeksterStepDefinition): VilkårsvurderingService {
    val vilkårsvurderingService = mockk<VilkårsvurderingService>()
    every { vilkårsvurderingService.hentAktivForBehandling(any()) } answers {
        val behandlingId = firstArg<Long>()
        dataFraCucumber.vilkårsvurderinger[behandlingId]!!
    }
    every { vilkårsvurderingService.hentAktivForBehandlingThrows(any()) } answers {
        val behandlingId = firstArg<Long>()
        dataFraCucumber.vilkårsvurderinger[behandlingId]!!
    }
    return vilkårsvurderingService
}
