package no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface KompetanseRepository : JpaRepository<Kompetanse, Long> {

    @Query("SELECT k FROM Kompetanse k WHERE k.behandlingId = :behandlingId")
    fun findByBehandlingId(behandlingId: Long): List<Kompetanse>
}
