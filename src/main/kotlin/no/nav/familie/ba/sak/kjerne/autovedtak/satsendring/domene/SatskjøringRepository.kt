package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface SatskjøringRepository : JpaRepository<Satskjøring, Long> {
    fun countByFerdigTidspunktIsNotNull(): Long
    fun findByFagsakId(fagsakId: Long): Satskjøring?

    @Query(
        value = """
            SELECT f.id, b.id
            FROM   Fagsak f
                join behandling b on f.id = b.fk_fagsak_id
                join arbeidsfordeling_pa_behandling arb on b.id = arb.fk_behandling_id
            WHERE NOT EXISTS (
                    SELECT  
                    FROM   satskjoering
                    WHERE  fk_fagsak_id = f.id
                ) AND f.status = 'LØPENDE' AND f.arkivert = false AND b.status <> 'AVSLUTTET'
            ORDER BY b.opprettet_tid;
        """,
        nativeQuery = true
    )
    fun finnSatskjøringerSomHarStoppetPgaÅpenBehandling(ofSize: Pageable): List<Pair<Long, Long>>
}
