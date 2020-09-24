package no.nav.familie.ba.sak.behandling.vilkår

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface BehandlingResultatRepository : JpaRepository<BehandlingResultat, Long> {

    @Query(value = "SELECT r FROM BehandlingResultat r JOIN r.behandling b WHERE b.id = :behandlingId")
    fun finnBehandlingResultater(behandlingId: Long): List<BehandlingResultat>

    @Query("SELECT r FROM BehandlingResultat r JOIN r.behandling b WHERE b.id = :behandlingId AND r.aktiv = true")
    fun findByBehandlingAndAktiv(behandlingId: Long): BehandlingResultat?
}
