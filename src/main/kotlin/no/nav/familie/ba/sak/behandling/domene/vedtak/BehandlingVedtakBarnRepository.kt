package no.nav.familie.ba.sak.behandling.domene.vedtak

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface BehandlingVedtakBarnRepository : JpaRepository<BehandlingVedtakBarn?, Long?> {
    @Query(value = "SELECT bvb FROM BehandlingVedtakBarn bvb JOIN bvb.behandlingVedtak bv WHERE bv.id = :behandlingVedtakId")
    fun finnBarnBeregningForVedtak(behandlingVedtakId: Long?): List<BehandlingVedtakBarn>
}