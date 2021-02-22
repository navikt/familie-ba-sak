package no.nav.familie.ba.sak.annenvurdering

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface AnnenVurderingRepository : JpaRepository<AnnenVurdering, Long> {

    @Query(value = "SELECT b FROM AnnenVurdering b WHERE b.personResultatId = :personResultatId AND b.type = :type")
    fun findBy(personResultatId: Long, type: AnnenVurderingType): AnnenVurdering?

    @Query(value = "SELECT b FROM AnnenVurdering b WHERE b.behandlingId = :behandlingId")
    fun findBy(behandlingId: Long): List<AnnenVurdering>
}