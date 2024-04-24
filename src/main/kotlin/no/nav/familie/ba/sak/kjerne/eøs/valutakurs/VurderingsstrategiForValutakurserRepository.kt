package no.nav.familie.ba.sak.kjerne.eøs.valutakurs

import org.springframework.data.jpa.repository.JpaRepository

interface VurderingsstrategiForValutakurserRepository : JpaRepository<VurderingsstrategiForValutakurserDB, Long> {
    fun findByBehandlingId(behandlingId: Long): VurderingsstrategiForValutakurserDB?
}
