package no.nav.familie.ba.sak.andreopplysninger

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface AndreVurderingerRepository : JpaRepository<AndreVurderinger, Long> {

    @Query(value = "SELECT b FROM AndreVurderinger b " +
                   "WHERE b.behandlingId = :behandlingId AND b.personResultatId = :personident AND b.type = :type")
    fun findBy(behandlingId: Long, personResultatId: Long, type: String): AndreVurderinger?
}