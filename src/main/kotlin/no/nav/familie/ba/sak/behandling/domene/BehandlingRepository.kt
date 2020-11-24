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


    /* Denne henter først siste iverksatte behandling på en løpende fagsak.
     * Finner så alle perioder på siste iverksatte behandling
     * Finner deretter første behandling en periode oppstod i, som er det som skal avstemmes
     */
    @Query(value = """select distinct periodeMedLøpendeAndel.behandlingAndelOppsto
                        /* Hent siste iverksatte behandlinger på løpende fagsaker. Tilkjent ytelse på disse behandlingene inneholder økonomitilstand (løpende andeler) på fagsak.  */
                        from (with sisteIverksatteBehandlingFraLøpendeFagsak as (
                            select f.id as fagsakId, max(b.id) as behandlingId
                            from behandling b
                                     inner join fagsak f on f.id = b.fk_fagsak_id
                                     inner join tilkjent_ytelse ty on b.id = ty.fk_behandling_id
                            where f.status = 'LØPENDE'
                              AND ty.utbetalingsoppdrag IS NOT NULL
                            GROUP BY fagsakId)
                            
                            /* Vi grupperer på fagsakId og periodeOffset. Velger så minste behandlingId i gruppa for å finne behandlingen hvor andelen først oppsto */
                              select behandlingPåLopendeFagsak.fk_fagsak_id,
                                     andelISisteIverksatte.periode_offset, /* Innad i en fagsak er dette en unik id hvor hver andel*/
                                     min(behandlingPåLopendeFagsak.id) as behandlingAndelOppsto
                              from behandling behandlingPåLopendeFagsak, andel_tilkjent_ytelse andelISisteIverksatte
                                  where 
                                  /* Grupperingen gjøres kun for behandlinger med andeler som finnes i økonomitilstand*/
                                  behandlingPåLopendeFagsak.id in 
                                    (select andel_tilkjent_ytelse.fk_behandling_id from andel_tilkjent_ytelse
                                        where andel_tilkjent_ytelse.periode_offset in
                                            (select andelfraRelevantBehandling.periode_offset from sisteIverksatteBehandlingFraLøpendeFagsak, andel_tilkjent_ytelse andelfraRelevantBehandling where andelfraRelevantBehandling.fk_behandling_id = sisteIverksatteBehandlingFraLøpendeFagsak.behandlingId))      
                                    AND andelISisteIverksatte.fk_behandling_id = behandlingPåLopendeFagsak.id
                              GROUP BY behandlingPåLopendeFagsak.fk_fagsak_id, andelISisteIverksatte.periode_offset) as periodeMedLøpendeAndel;
                        """,
           nativeQuery = true)
    fun finnBehandlingerMedLøpendeAndel(): List<Long>

    @Query("SELECT b FROM Behandling b JOIN b.fagsak f WHERE f.id = :fagsakId AND b.status = 'AVSLUTTET'")
    fun findByFagsakAndAvsluttet(fagsakId: Long): List<Behandling>
}