package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.cucumber.VedtaksperioderOgBegrunnelserStepDefinition
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService

fun mockVilkårService(dataFraCucumber: VedtaksperioderOgBegrunnelserStepDefinition): VilkårService {
    val vilkårService = mockk<VilkårService>()
    every { vilkårService.hentVilkårsvurdering(any()) } answers {
        val behandlingsId = firstArg<Long>()
        dataFraCucumber.vilkårsvurderinger[behandlingsId]!!
    }
    every { vilkårService.hentVilkårsvurderingThrows(any()) } answers {
        val behandlingsId = firstArg<Long>()
        dataFraCucumber.vilkårsvurderinger[behandlingsId]!!
    }
    return vilkårService
}
