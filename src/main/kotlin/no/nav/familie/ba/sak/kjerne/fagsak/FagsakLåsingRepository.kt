package no.nav.familie.ba.sak.kjerne.fagsak

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface FagsakLåsingRepository : JpaRepository<FagsakLåsing, Long> {
    @Query(
        """
        SELECT fl FROM FagsakLaasing fl
        WHERE fl.fagsak.id = :fagsakId
          AND fl.aktiv = true
        """,
    )
    fun finnAktivFagsakLåsing(
        @Param("fagsakId") fagsakId: Long,
    ): FagsakLåsing?
}
