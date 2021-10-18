package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PersonRepository : JpaRepository<Person, Long> {

    @Query(
        "SELECT p FROM Person p" +
            " WHERE p.personIdent = :personIdent"
    )
    fun findByPersonIdent(personIdent: PersonIdent): List<Person>

    @Query(
        "SELECT p FROM Person p WHERE p.personIdent in :personIdenter"
    )
    fun findByPersonIdenter(personIdenter: List<PersonIdent>): List<Person>
}
