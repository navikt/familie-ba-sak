package no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger

import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface PersonRepository : JpaRepository<Person, Long> {

    @Query("SELECT p FROM Person p" +
            " WHERE p.personIdent = :personIdent")
    fun findByPersonIdent(personIdent: PersonIdent): List<Person>
}
