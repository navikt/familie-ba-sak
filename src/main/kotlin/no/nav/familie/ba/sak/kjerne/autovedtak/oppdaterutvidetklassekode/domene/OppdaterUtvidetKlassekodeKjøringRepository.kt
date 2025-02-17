package no.nav.familie.ba.sak.kjerne.autovedtak.oppdaterutvidetklassekode.domene

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface OppdaterUtvidetKlassekodeKjøringRepository : JpaRepository<OppdaterUtvidetKlassekodeKjøring, Long> {
    @Query("SELECT oukk FROM OppdaterUtvidetKlassekodeKjøring oukk WHERE oukk.brukerNyKlassekode = false and oukk.status = Status.IKKE_UTFØRT ")
    fun finnRelevanteOppdaterUtvidetKlassekodeKjøringer(
        pageable: Pageable,
    ): Page<OppdaterUtvidetKlassekodeKjøring>

    @Transactional
    @Modifying
    @Query("UPDATE OppdaterUtvidetKlassekodeKjøring SET brukerNyKlassekode = true, status = Status.UTFØRT WHERE fagsakId = :fagsakId")
    fun settBrukerNyKlassekodeTilTrueOgStatusTilUtført(
        fagsakId: Long,
    )

    @Transactional
    @Modifying
    @Query("UPDATE OppdaterUtvidetKlassekodeKjøring SET status = :status WHERE fagsakId = :fagsakId")
    fun oppdaterStatus(
        fagsakId: Long,
        status: Status,
    )

    fun deleteByFagsakId(fagsakId: Long)
}
