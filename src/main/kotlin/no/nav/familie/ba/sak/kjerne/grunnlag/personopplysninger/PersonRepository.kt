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
            SELECT DISTINCT f
            FROM Person p
                     JOIN PersonopplysningGrunnlag po ON po.id = p.personopplysningGrunnlag.id
                     JOIN Behandling b ON b.id = po.behandlingId
                     JOIN Fagsak f ON f.id = b.fagsak.id
            WHERE p.aktør = :aktør
              AND po.aktiv = true
              AND f.arkivert = false
        """,
    )
    fun findFagsakerByAktør(aktør: Aktør): List<Fagsak>
}
