package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import no.nav.familie.ba.sak.kjerne.personident.Aktør
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PersonRepository : JpaRepository<Person, Long> {

    @Query(
        "SELECT p FROM Person p" +
            " WHERE p.aktør = :aktør"
    )
    fun findByAktør(aktør: Aktør): List<Person>

    @Query(
        "SELECT p FROM Person p WHERE p.aktør in :aktører"
    )
    fun findByAktører(aktører: List<Aktør>): List<Person>
}
