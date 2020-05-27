package no.nav.familie.ba.sak.behandling.fagsak

import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import javax.persistence.LockModeType

@Repository
interface FagsakPersonRepository : JpaRepository<FagsakPerson, Long> {

    @Lock(LockModeType.NONE)
    @Query(value = "SELECT DISTINCT(f.fagsak) FROM FagsakPerson f WHERE f.personIdent in :personIdenter")
    fun finnFagsak(personIdenter: Set<PersonIdent>): Fagsak?

}
