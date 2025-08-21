package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PersonRepository : JpaRepository<Person, Long> {
    @Query(
        "SELECT p FROM Person p" +
            " WHERE p.aktør = :aktør",
    )
    fun findByAktør(aktør: Aktør): List<Person>

    @Query(
        """
            SELECT DISTINCT ON(f.id) f.*
            FROM po_person p
                     JOIN gr_personopplysninger pog ON pog.id = p.fk_gr_personopplysninger_id
                     JOIN behandling b ON b.id = pog.fk_behandling_id
                     JOIN Fagsak f ON f.id = b.fk_fagsak_id
            WHERE p.fk_aktoer_id = :aktørId
              AND pog.aktiv = true
              AND f.arkivert = false
        """,
        nativeQuery = true,
    )
    fun findFagsakerByAktør(aktørId: String): List<Fagsak>
}
