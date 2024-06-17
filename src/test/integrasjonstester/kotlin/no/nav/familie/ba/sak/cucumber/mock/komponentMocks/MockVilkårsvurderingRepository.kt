package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.cucumber.BegrunnelseTeksterStepDefinition
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository

fun mockVilkårsvurderingRepository(dataFraCucumber: BegrunnelseTeksterStepDefinition): VilkårsvurderingRepository {
    val vilkårsvurderingRepository = mockk<VilkårsvurderingRepository>()
    every { vilkårsvurderingRepository.findByBehandlingAndAktiv(any()) } answers {
        val behandlingId = firstArg<Long>()
        dataFraCucumber.vilkårsvurderinger[behandlingId]!!
    }

    every { vilkårsvurderingRepository.saveAndFlush(any()) } answers {
        val vilkårsvurdering = firstArg<Vilkårsvurdering>()
        dataFraCucumber.vilkårsvurderinger[vilkårsvurdering.behandling.id] = vilkårsvurdering
        vilkårsvurdering
    }

    every { vilkårsvurderingRepository.save(any()) } answers {
        val vilkårsvurdering = firstArg<Vilkårsvurdering>()
        dataFraCucumber.vilkårsvurderinger[vilkårsvurdering.behandling.id] = vilkårsvurdering
        vilkårsvurdering
    }

    return vilkårsvurderingRepository
}
