package no.nav.familie.ba.sak.annenvurdering

import no.nav.familie.ba.sak.behandling.vilkår.PersonResultat
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface AnnenVurderingRepository : JpaRepository<AnnenVurdering, Long> {

    @Query(value = "SELECT b FROM AnnenVurdering b WHERE b.personResultatAV = :personResultat AND b.type = :type")
    fun findBy(personResultat: PersonResultat, type: AnnenVurderingType): AnnenVurdering?
}