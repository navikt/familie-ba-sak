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
    @Query(value = """select distinct perioderPåFagsak.behandlingPeriodeOppsto
                        from (with sisteIverksatteBehandlingFraLøpendeFagsak as (
                            select f.id as fagsakId, max(b.id) as behandlingId
                            from behandling b
                                     inner join fagsak f on f.id = b.fk_fagsak_id
                                     inner join tilkjent_ytelse ty on b.id = ty.fk_behandling_id
                            where f.status = 'LØPENDE'
                              AND ty.utbetalingsoppdrag IS NOT NULL
                            GROUP BY fagsakId)
                              select alleIverksatteBehandlingerPåFagsak.fk_fagsak_id,
                                     alleAndelerPåIverksatteBehandlinger.periode_offset,
                                     min(alleIverksatteBehandlingerPåFagsak.id) as behandlingPeriodeOppsto
                              from behandling alleIverksatteBehandlingerPåFagsak, andel_tilkjent_ytelse alleAndelerPåIverksatteBehandlinger
                                  where alleIverksatteBehandlingerPåFagsak.id in 
                                    (select periodeoffsetFraSisteIverksatteBehandling.fk_behandling_id from andel_tilkjent_ytelse periodeoffsetFraSisteIverksatteBehandling 
                                        where periodeoffsetFraSisteIverksatteBehandling.periode_offset in
                                            (select atyPåsisteIverksatteBeh.periode_offset from sisteIverksatteBehandlingFraLøpendeFagsak fa, andel_tilkjent_ytelse atyPåsisteIverksatteBeh where atyPåsisteIverksatteBeh.fk_behandling_id = fa.behandlingId))
                              AND alleAndelerPåIverksatteBehandlinger.fk_behandling_id = alleIverksatteBehandlingerPåFagsak.id
                              GROUP BY alleIverksatteBehandlingerPåFagsak.fk_fagsak_id, alleAndelerPåIverksatteBehandlinger.periode_offset) as perioderPåFagsak;
                        """,
           nativeQuery = true)
    fun finnBehandlingerMedLøpendeAndel(): List<Long>

    @Query("SELECT b FROM Behandling b JOIN b.fagsak f WHERE f.id = :fagsakId AND b.status = 'AVSLUTTET'")
    fun findByFagsakAndAvsluttet(fagsakId: Long): List<Behandling>
}