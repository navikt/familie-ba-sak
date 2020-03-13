package no.nav.familie.ba.sak.behandling.domene.personopplysninger

import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PersonRepository : JpaRepository<Person, Long> {

    @Query("SELECT p FROM Person p JOIN p.personopplysningGrunnlag pog " +
           "WHERE pog.id = :personopplysningGrunnlagId AND p.personIdent = :personIdent")
    fun findByPersonIdentAndPersonopplysningGrunnlag(personIdent: PersonIdent, personopplysningGrunnlagId: Long): Person?

    @Query("SELECT p FROM Person p" +
            " WHERE p.personIdent = :personIdent")
    fun findByPersonIdent(personIdent: PersonIdent): List<Person>
}
