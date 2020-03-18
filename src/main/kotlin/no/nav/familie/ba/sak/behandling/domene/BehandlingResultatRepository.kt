package no.nav.familie.ba.sak.behandling.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface BehandlingResultatRepository : JpaRepository<BehandlingResultat, Long> {
    @Query(value = "SELECT r FROM BehandlingResultat r WHERE r.id = :behandlingResultatId")
    fun finnBehandlingResultat(behandlingId: Long): BehandlingResultat

    @Query(value = "SELECT r FROM BehandlingResultat r JOIN r.behandling b WHERE b.id = :behandlingId")
    fun finnBehandlingResultater(fagsakId: Long): List<BehandlingResultat>

    @Query("SELECT r FROM BehandlingResultat r JOIN r.behandling b WHERE b.id = :behandlingId AND r.aktiv = true")
    fun findByBehandlingAndAktiv(behandlingId: Long): BehandlingResultat?
}
