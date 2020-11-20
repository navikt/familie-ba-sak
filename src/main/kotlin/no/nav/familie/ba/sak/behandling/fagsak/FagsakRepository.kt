package no.nav.familie.ba.sak.behandling.fagsak

import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*
import javax.persistence.LockModeType

@Repository
interface FagsakRepository : JpaRepository<Fagsak, Long> {

    @Lock(LockModeType.PESSIMISTIC_FORCE_INCREMENT)
    fun save(fagsak: Fagsak): Fagsak

    @Lock(LockModeType.NONE)
    override fun findById(id: Long): Optional<Fagsak>

    @Lock(LockModeType.NONE)
    @Query(value = "SELECT f FROM Fagsak f WHERE f.id = :fagsakId")
    fun finnFagsak(fagsakId: Long): Fagsak?

    @Lock(LockModeType.NONE)
    @Query(value = "SELECT f FROM Fagsak f, FagsakPerson fp WHERE f.id = fp.fagsak.id and fp.personIdent = :personIdent")
    fun finnFagsakForPersonIdent(personIdent: PersonIdent): Fagsak?

    @Lock(LockModeType.NONE)
    @Query(value = "SELECT f from Fagsak f WHERE f.status = 'LØPENDE'")
    fun finnLøpendeFagsaker(): List<Fagsak>

    @Query(value = """update fagsak
                        set status='AVSLUTTET'
                        where fagsak.id in (
                            with sisteIverksatte as (
                                select b.fk_fagsak_id as fagsakId, max(b.id) as behandlingId
                                from behandling b
                                         inner join tilkjent_ytelse ty on b.id = ty.fk_behandling_id
                                         inner join fagsak f on f.id = b.fk_fagsak_id
                                where ty.utbetalingsoppdrag IS NOT NULL
                                  and f.status = 'LØPENDE'
                                group by b.id)
                            select sisteIverksatte.fagsakId
                            from sisteIverksatte
                                     inner join tilkjent_ytelse ty on sisteIverksatte.behandlingId = ty.fk_behandling_id
                            where ty.stonad_tom < now())""", // TODO: Må valideres
    nativeQuery = true)
    fun oppdaterLøpendeStatusPåFagsaker(): Int
}
