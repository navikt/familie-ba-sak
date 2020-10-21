package no.nav.familie.ba.sak.opplysningsplikt

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface OpplysningspliktRepository : JpaRepository<Opplysningsplikt, Long> {
    @Query("SELECT o FROM Opplysningsplikt o WHERE o.behandlingId = :behandlingId")
    fun findByBehandlingId(behandlingId: Long): Opplysningsplikt?
}