package no.nav.familie.ba.sak.ekstern.pensjon

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface FerdigstiltBehandlingForPensjonRepository : JpaRepository<FerdigstiltBehandlingForPensjon, Long> {
    // Låsen holdes til commit, slik at høyeste sekvensnummer + 1 gir sekvensnummer uten hull også ved rollback.
    @Modifying
    @Query(value = "LOCK TABLE ferdigstilt_behandling_for_pensjon IN EXCLUSIVE MODE", nativeQuery = true)
    fun låsTabell()

    @Query("SELECT COALESCE(MAX(f.sekvensnummer), 0) FROM FerdigstiltBehandlingForPensjon f")
    fun finnHøyesteSekvensnummer(): Long

    fun findByBehandlingId(behandlingId: Long): FerdigstiltBehandlingForPensjon?
}
