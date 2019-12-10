package no.nav.familie.ba.sak.behandling.domene.vedtak

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface BehandlingVedtakRepository : JpaRepository<BehandlingVedtak?, Long?> {
    @Query(value = "SELECT bv FROM BehandlingVedtak bv JOIN bv.behandling b WHERE b.id = :behandlingId")
    fun finnVedtakForBehandling(behandlingId: Long?): List<BehandlingVedtak?>

    @Query(value = "SELECT bv FROM BehandlingVedtak bv JOIN bv.behandling b WHERE b.id = :behandlingId")
    fun finnBehandlingVedtak(behandlingId: Long?): BehandlingVedtak?

    @Query("SELECT bv FROM BehandlingVedtak bv JOIN bv.behandling b WHERE b.id = :behandlingId AND bv.aktiv = true")
    fun findByBehandlingAndAktiv(behandlingId: Long?): BehandlingVedtak?
}