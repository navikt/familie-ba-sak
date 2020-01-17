package no.nav.familie.ba.sak.behandling.domene

import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*
import javax.persistence.LockModeType

@Repository
interface FagsakRepository : JpaRepository<Fagsak?, Long?> {
    @Lock(LockModeType.PESSIMISTIC_FORCE_INCREMENT)
    fun save(fagsak: Fagsak): Fagsak

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    override fun findById(id: Long): Optional<Fagsak?>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(value = "SELECT f FROM Fagsak f WHERE f.id = :fagsakId")
    fun finnFagsak(fagsakId: Long?): Fagsak?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(value = "SELECT f FROM Fagsak f WHERE f.personIdent = :personIdent")
    fun finnFagsakForPersonIdent(personIdent: PersonIdent): Fagsak?
}