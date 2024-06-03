package no.nav.familie.ba.sak.cucumber.mock.komponentMocks

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.VurderingsstrategiForValutakurser
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.VurderingsstrategiForValutakurserDB
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.VurderingsstrategiForValutakurserRepository

fun mockVurderingsstrategiForValutakurserRepository(): VurderingsstrategiForValutakurserRepository {
    val vurderingsstrategiForValutakurserRepository = mockk<VurderingsstrategiForValutakurserRepository>()
    every { vurderingsstrategiForValutakurserRepository.findByBehandlingId(any()) } answers {
        val behandlingId = firstArg<Long>()
        VurderingsstrategiForValutakurserDB(behandlingId = behandlingId, vurderingsstrategiForValutakurser = VurderingsstrategiForValutakurser.AUTOMATISK)
    }
    return vurderingsstrategiForValutakurserRepository
}
