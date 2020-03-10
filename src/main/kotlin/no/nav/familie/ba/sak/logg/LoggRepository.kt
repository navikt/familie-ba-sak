package no.nav.familie.ba.sak.logg

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface LoggRepository: JpaRepository<Logg, Long> {
    @Query(value = "SELECT l FROM Logg l WHERE l.behandlingId = :behandlingId")
    fun hentLoggForBehandling(behandlingId: Long): List<Logg>
}