package no.nav.familie.ba.sak.behandling.domene.vedtak

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface BehandlingVedtakRepository : JpaRepository<BehandlingVedtak?, Long?> {
    @Query(value = "SELECT bv FROM BehandlingVedtak bv WHERE bv.behandlingId = :behandlingId")
    fun finnBehandlingVedtak(behandlingId: Long?): BehandlingVedtak?
}