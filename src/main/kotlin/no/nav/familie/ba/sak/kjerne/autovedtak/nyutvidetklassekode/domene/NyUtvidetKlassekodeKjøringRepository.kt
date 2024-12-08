package no.nav.familie.ba.sak.kjerne.autovedtak.nyutvidetklassekode.domene

import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface NyUtvidetKlassekodeKjøringRepository : JpaRepository<NyUtvidetKlassekodeKjøring, Long> {
    fun findByBrukerNyKlassekodeIsFalse(limit: Limit = Limit.unlimited()): List<NyUtvidetKlassekodeKjøring>

    @Modifying
    @Query("UPDATE NyUtvidetKlassekodeKjøring SET brukerNyKlassekode = true WHERE fagsakId = :fagsakId")
    fun settBrukerNyKlassekodeTilTrue(fagsakId: Long)

    fun deleteByFagsakId(fagsakId: Long)
}
