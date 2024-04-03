package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.cucumber.BegrunnelseTeksterStepDefinition
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository

fun mockVilkårsvurderingRepository(dataFraCucumber: BegrunnelseTeksterStepDefinition): VilkårsvurderingRepository {
    val vilkårsvurderingRepository = mockk<VilkårsvurderingRepository>()
    every { vilkårsvurderingRepository.findByBehandlingAndAktiv(any()) } answers {
        val behandlingId = firstArg<Long>()
        dataFraCucumber.vilkårsvurderinger[behandlingId]!!
    }
    return vilkårsvurderingRepository
}
