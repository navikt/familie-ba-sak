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

    @Query("SELECT b FROM Behandling b JOIN b.fagsak f WHERE f.id = :fagsakId AND b.gjeldendeForFremtidigUtbetaling = true")
    fun findByFagsakAndGjeldendeForUtbetaling(fagsakId: Long): List<Behandling>

    @Query("""select distinct perioderPåFagsak.behandlingPeriodeOppsto
                        from (with behandlingFraLøpendeFagsak as (
                            select f.id as fagsakId, max(b.id) as behandlingId
                            from behandling b
                                     inner join fagsak f on f.id = b.fk_fagsak_id
                                     inner join tilkjent_ytelse ty on b.id = ty.fk_behandling_id
                            where f.status = 'LØPENDE'
                              AND ty.utbetalingsoppdrag IS NOT NULL
                            GROUP BY fagsakId)
                              select behandlingFraLøpendeFagsak.fagsakId,
                                     aty.periode_offset,
                                     min(behandlingFraLøpendeFagsak.behandlingId) as behandlingPeriodeOppsto
                              from behandlingFraLøpendeFagsak
                                       inner join andel_tilkjent_ytelse aty on behandlingFraLøpendeFagsak.behandlingId = aty.fk_behandling_id
                              GROUP BY behandlingFraLøpendeFagsak.fagsakId, aty.periode_offset) as perioderPåFagsak;
                        """, nativeQuery = true)
    fun finnBehandlingerMedLøpendeAndel(): List<Long>

    @Query("SELECT b FROM Behandling b JOIN b.fagsak f WHERE f.id = :fagsakId AND b.status = 'AVSLUTTET'")
    fun findByFagsakAndAvsluttet(fagsakId: Long): List<Behandling>
}

/*



select distinct perioderPåFagsak.behandlingPeriodeOppsto
from (with behandlingFraLøpendeFagsak as (
    select f.id as fagsakId, max(b.id) as behandlingId
    from behandling b
             inner join fagsak f on f.id = b.fk_fagsak_id
             inner join tilkjent_ytelse ty on b.id = ty.fk_behandling_id
    where f.status = 'LØPENDE'
      AND ty.utbetalingsoppdrag IS NOT NULL
    GROUP BY fagsakId)
      select behandlingFraLøpendeFagsak.fagsakId,
             aty.periode_offset,
             min(behandlingFraLøpendeFagsak.behandlingId) as behandlingPeriodeOppsto
      from behandlingFraLøpendeFagsak
               inner join andel_tilkjent_ytelse aty on behandlingFraLøpendeFagsak.behandlingId = aty.fk_behandling_id
      GROUP BY behandlingFraLøpendeFagsak.fagsakId, aty.periode_offset) as perioderPåFagsak;



 */