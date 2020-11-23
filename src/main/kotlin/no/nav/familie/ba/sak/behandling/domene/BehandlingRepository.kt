package no.nav.familie.ba.sak.behandling.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface BehandlingRepository : JpaRepository<Behandling, Long> {

    @Query(value = "SELECT b FROM Behandling b WHERE b.id = :behandlingId")
    fun finnBehandling(behandlingId: Long): Behandling

    @Query(value = "SELECT b FROM Behandling b JOIN b.fagsak f WHERE f.id = :fagsakId")
    fun finnBehandlinger(fagsakId: Long): List<Behandling>

    @Query("SELECT b FROM Behandling b JOIN b.fagsak f WHERE f.id = :fagsakId AND b.aktiv = true")
    fun findByFagsakAndAktiv(fagsakId: Long): Behandling?

    @Query(value = """select distinct perioderPåFagsak.behandlingPeriodeOppsto
                        from (with behandlingFraLøpendeFagsak as (
                            select f.id as fagsakId, max(b.id) as behandlingId
                            from behandling b
                                     inner join fagsak f on f.id = b.fk_fagsak_id
                                     inner join tilkjent_ytelse ty on b.id = ty.fk_behandling_id
                            where f.status = 'LØPENDE'
                              AND ty.utbetalingsoppdrag IS NOT NULL
                            GROUP BY fagsakId)
                              select beh.fk_fagsak_id,
                                     andel.periode_offset,
                                     min(beh.id) as behandlingPeriodeOppsto
                              from behandling beh, andel_tilkjent_ytelse andel
                                  where beh.id in (select aty1.fk_behandling_id from andel_tilkjent_ytelse aty1 where aty1.periode_offset in
                                                                                                                      (select aty3.periode_offset from behandlingFraLøpendeFagsak fa, andel_tilkjent_ytelse aty3
                                                                                                                          where aty3.fk_behandling_id = fa.behandlingId))
                                  AND andel.fk_behandling_id = beh.id
                              GROUP BY beh.fk_fagsak_id, andel.periode_offset) as perioderPåFagsak;
                        """,
           nativeQuery = true)
    fun finnBehandlingerMedLøpendeAndel(): List<Long>

    @Query("SELECT b FROM Behandling b JOIN b.fagsak f WHERE f.id = :fagsakId AND b.status = 'AVSLUTTET'")
    fun findByFagsakAndAvsluttet(fagsakId: Long): List<Behandling>
}