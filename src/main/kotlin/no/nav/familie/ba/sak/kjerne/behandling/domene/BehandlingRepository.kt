package no.nav.familie.ba.sak.kjerne.behandling.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import javax.persistence.LockModeType

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
    @Query(value = """WITH sisteiverksattebehandlingfraløpendefagsak AS (
                            SELECT f.id AS fagsakid, MAX(b.id) AS behandlingid
                            FROM behandling b
                                   INNER JOIN fagsak f ON f.id = b.fk_fagsak_id
                                   INNER JOIN tilkjent_ytelse ty ON b.id = ty.fk_behandling_id
                            WHERE f.status = 'LØPENDE'
                              AND ty.utbetalingsoppdrag IS NOT NULL
                            GROUP BY fagsakid)
                        
                        SELECT behandlingid FROM sisteiverksattebehandlingfraløpendefagsak""",
           nativeQuery = true)
    fun finnSisteIverksatteBehandlingFraLøpendeFagsaker(): List<Long>

    @Query("""select b from Behandling b
                           inner join TilkjentYtelse ty on b.id = ty.behandling.id
                        where b.fagsak.id = :fagsakId AND ty.utbetalingsoppdrag IS NOT NULL""")
    fun finnIverksatteBehandlinger(fagsakId: Long): List<Behandling>

    @Query("SELECT b FROM Behandling b JOIN b.fagsak f WHERE f.id = :fagsakId AND b.status = 'AVSLUTTET'")
    fun findByFagsakAndAvsluttet(fagsakId: Long): List<Behandling>

    @Lock(LockModeType.NONE)
    @Query("SELECT count(*) FROM Behandling b WHERE NOT b.status = 'AVSLUTTET'")
    fun finnAntallBehandlingerIkkeAvsluttet(): Long

    /**
     * Gjenbruker finnSisteIverksatteBehandlingFraLøpendeFagsaker og filtrerer på beløp og datoer
     */
    @Query(value = """WITH sisteiverksattebehandlingfraløpendefagsak AS (
                            SELECT f.id AS fagsakid, MAX(b.id) AS behandlingid
                            FROM behandling b
                                   INNER JOIN fagsak f ON f.id = b.fk_fagsak_id
                                   INNER JOIN tilkjent_ytelse ty ON b.id = ty.fk_behandling_id
                            WHERE f.status = 'LØPENDE'
                              AND ty.utbetalingsoppdrag IS NOT NULL
                            GROUP BY fagsakid)
                        
                        SELECT DISTINCT aty.fk_behandling_id
                        FROM andel_tilkjent_ytelse aty
                        WHERE aty.fk_behandling_id IN (SELECT behandlingid FROM sisteiverksattebehandlingfraløpendefagsak)
                            AND aty.belop = 1354
                            AND aty.stonad_fom <= TO_TIMESTAMP('01-09-2021', 'DD-MM-YYYY')
                            AND aty.stonad_tom >= TO_TIMESTAMP('30-09-2021', 'DD-MM-YYYY')""",
           nativeQuery = true)
    fun finnBehandlingerSomSkalSatsendresSeptember21(): List<Long>
}