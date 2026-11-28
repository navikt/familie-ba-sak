package no.nav.familie.ba.sak.kjerne.brev.mottaker

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface BrevmottakerRepository : JpaRepository<BrevmottakerDb, Long> {
    @Query(value = "SELECT b FROM Brevmottaker b WHERE b.behandlingId = :behandlingId ORDER BY b.id")
    fun finnBrevmottakerePåBehandling(behandlingId: Long): List<BrevmottakerDb>
}
