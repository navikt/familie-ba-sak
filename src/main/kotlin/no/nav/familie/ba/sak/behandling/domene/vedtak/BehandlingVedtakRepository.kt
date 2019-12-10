package no.nav.familie.ba.sak.behandling.domene.vedtak

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface BehandlingVedtakRepository : JpaRepository<BehandlingVedtak?, Long?> {
    @Query(value = "SELECT bv FROM BehandlingVedtak bv WHERE behandlingId = :behandlingId")
    fun finnVedtakForBehandling(behandlingId: Long?): List<BehandlingVedtak?>

    @Query(value = "SELECT bv FROM BehandlingVedtak bv WHERE behandlingId = :behandlingId")
    fun finnBehandlingVedtak(behandlingId: Long?): BehandlingVedtak?

    @Query("SELECT bv FROM BehandlingVedtak bv WHERE behandlingId = :behandlingId AND bv.aktiv = true")
    fun findByBehandlingAndAktiv(behandlingId: Long?): BehandlingVedtak?
}