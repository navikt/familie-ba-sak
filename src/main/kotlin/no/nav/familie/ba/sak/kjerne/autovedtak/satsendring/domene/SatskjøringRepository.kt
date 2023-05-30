package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface SatskjøringRepository : JpaRepository<Satskjøring, Long> {
    fun countByFerdigTidspunktIsNotNull(): Long
    fun findByFagsakId(fagsakId: Long): Satskjøring?

    @Query(
        value = """
            SELECT f.id as fagsakId, b.id as behandlingId
            FROM   Fagsak f
                join behandling b on f.id = b.fk_fagsak_id
            WHERE NOT EXISTS (
                    SELECT
                    FROM   satskjoering
                    WHERE  fk_fagsak_id = f.id
                ) AND f.status = 'LØPENDE' AND f.arkivert = false AND b.status <> 'AVSLUTTET'
            ORDER BY b.aktivert_tid
        """,
        nativeQuery = true,
    )
    fun finnSatskjøringerSomHarStoppetPgaÅpenBehandling(): List<SatskjøringÅpenBehandling>
}

interface SatskjøringÅpenBehandling {
    val fagsakId: Long
    val behandlingId: Long
}
