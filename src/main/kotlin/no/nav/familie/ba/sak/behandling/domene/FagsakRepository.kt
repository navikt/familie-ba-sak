package no.nav.familie.ba.sak.behandling.domene

import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface FagsakRepository : JpaRepository<Fagsak?, Long?> {
    @Query(value = "SELECT f FROM Fagsak f WHERE f.id = :fagsakId")
    fun finnFagsak(fagsakId: Long?): Fagsak?

    @Query(value = "SELECT f FROM Fagsak f WHERE f.personIdent = :personIdent")
    fun finnFagsakForPersonIdent(personIdent: PersonIdent): Fagsak?
}