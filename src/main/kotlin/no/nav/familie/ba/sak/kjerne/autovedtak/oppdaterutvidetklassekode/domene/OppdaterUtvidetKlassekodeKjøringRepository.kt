package no.nav.familie.ba.sak.kjerne.autovedtak.oppdaterutvidetklassekode.domene

import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface OppdaterUtvidetKlassekodeKjøringRepository : JpaRepository<OppdaterUtvidetKlassekodeKjøring, Long> {
    fun findByBrukerNyKlassekodeIsFalse(limit: Limit = Limit.unlimited()): List<OppdaterUtvidetKlassekodeKjøring>

    @Modifying
    @Query("UPDATE OppdaterUtvidetKlassekodeKjøring SET brukerNyKlassekode = true WHERE fagsakId = :fagsakId")
    fun settBrukerNyKlassekodeTilTrue(fagsakId: Long)

    fun deleteByFagsakId(fagsakId: Long)
}
