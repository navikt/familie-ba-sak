package no.nav.familie.ba.sak.kjerne.behandling.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface BehandlingMigreringsinfoRepository : JpaRepository<BehandlingMigreringsinfo, Long> {

    @Query(
        """SELECT MAX(bm.migreringsdato) FROM BehandlingMigreringsinfo bm 
            INNER JOIN Behandling b ON bm.behandling.id = b.id 
            INNER JOIN Fagsak f ON b.id = f.id 
            WHERE f.id=:fagsakId"""
    )
    fun finnSisteMigreringsdatoPåFagsak(fagsakId: Long): LocalDate

    @Query("SELECT bm.migreringsdato FROM BehandlingMigreringsinfo bm INNER JOIN Behandling b ON bm.behandling.id = b.id ")
    fun findByBehandlingId(behandlingId: Long): LocalDate
}
