package no.nav.familie.ba.sak.kjerne.behandling.domene

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import java.math.BigInteger
import java.time.LocalDateTime
import java.time.YearMonth
import javax.persistence.LockModeType

interface BehandlingRepository : JpaRepository<Behandling, Long> {

    @Query(value = "SELECT b FROM Behandling b WHERE b.id = :behandlingId")
    fun finnBehandling(behandlingId: Long): Behandling

    @Query(value = "SELECT b FROM Behandling b JOIN b.fagsak f WHERE f.id = :fagsakId AND f.arkivert = false")
    fun finnBehandlinger(fagsakId: Long): List<Behandling>

    @Query("SELECT b FROM Behandling b JOIN b.fagsak f WHERE f.id = :fagsakId AND b.aktiv = true AND f.arkivert = false")
    fun findByFagsakAndAktiv(fagsakId: Long): Behandling?

    @Query("SELECT b FROM Behandling b JOIN b.fagsak f WHERE f.id = :fagsakId AND b.aktiv = true AND b.status <> 'AVSLUTTET' AND f.arkivert = false")
    fun findByFagsakAndAktivAndOpen(fagsakId: Long): Behandling?

    @Query(
        value = """WITH sisteiverksattebehandlingfraløpendefagsak AS (
                        SELECT f.id AS fagsakid, MAX(b.id) AS behandlingid
                        FROM behandling b
                                 INNER JOIN fagsak f ON f.id = b.fk_fagsak_id
                                 INNER JOIN tilkjent_ytelse ty ON b.id = ty.fk_behandling_id
                        WHERE f.status = 'LØPENDE'
                          AND ty.utbetalingsoppdrag IS NOT NULL
                          AND f.arkivert = false
                        GROUP BY fagsakid)
                        select sum(aty.kalkulert_utbetalingsbelop) from andel_tilkjent_ytelse aty
                        where aty.stonad_fom <= :måned
                          AND aty.stonad_tom >= :måned
                        AND aty.fk_behandling_id in (SELECT behandlingid FROM sisteiverksattebehandlingfraløpendefagsak)""",
        nativeQuery = true
    )
    fun hentTotalUtbetalingForMåned(måned: LocalDateTime): Long

    /* Denne henter først siste iverksatte behandling på en løpende fagsak.
     * Finner så alle perioder på siste iverksatte behandling
     * Finner deretter første behandling en periode oppstod i, som er det som skal avstemmes
     */
    @Query(
        value = """WITH sisteiverksattebehandlingfraløpendefagsak AS (
                            SELECT f.id AS fagsakid, MAX(b.id) AS behandlingid
                            FROM behandling b
                                   INNER JOIN fagsak f ON f.id = b.fk_fagsak_id
                                   INNER JOIN tilkjent_ytelse ty ON b.id = ty.fk_behandling_id
                            WHERE f.status = 'LØPENDE'
                              AND ty.utbetalingsoppdrag IS NOT NULL
                              AND f.arkivert = false
                            GROUP BY fagsakid)
                        
                        SELECT behandlingid FROM sisteiverksattebehandlingfraløpendefagsak""",
        nativeQuery = true
    )
    fun finnSisteIverksatteBehandlingFraLøpendeFagsaker(): List<Long>

    @Query(
        value = """WITH sisteiverksattebehandlingfraløpendefagsak AS (
                            SELECT f.id AS fagsakid, MAX(b.id) AS behandlingid
                            FROM behandling b
                                   INNER JOIN fagsak f ON f.id = b.fk_fagsak_id
                                   INNER JOIN tilkjent_ytelse ty ON b.id = ty.fk_behandling_id
                            WHERE f.status = 'LØPENDE'
                              AND ty.utbetalingsoppdrag IS NOT NULL
                              AND f.arkivert = false
                            GROUP BY fagsakid)
                        
                        SELECT behandlingid FROM sisteiverksattebehandlingfraløpendefagsak""",
        nativeQuery = true
    )
    fun finnSisteIverksatteBehandlingFraLøpendeFagsaker(page: Pageable): Page<BigInteger>

    @Query(
        """select b from Behandling b
                           inner join TilkjentYtelse ty on b.id = ty.behandling.id
                        where b.fagsak.id = :fagsakId AND ty.utbetalingsoppdrag IS NOT NULL"""
    )
    fun finnIverksatteBehandlinger(fagsakId: Long): List<Behandling>

    @Query(
        """
            select b from Behandling b 
                            inner join BehandlingStegTilstand  bst on b.id = bst.behandling.id
                            inner join TilkjentYtelse ty on b.id = ty.behandling.id
                            where b.fagsak.id = :fagsakId and bst.behandlingSteg = 'BESLUTTE_VEDTAK' AND bst.behandlingStegStatus = 'UTFØRT'
                                and ty.utbetalingsoppdrag IS NULL 
        """
    )
    fun finnGodkjenteBehandlingerSomHolderPåÅIverksettes(fagsakId: Long): List<Behandling>

    @Query(
        """select b from Behandling b
                           inner join BehandlingStegTilstand bst on b.id = bst.behandling.id
                        where b.fagsak.id = :fagsakId AND bst.behandlingSteg = 'BESLUTTE_VEDTAK' AND bst.behandlingStegStatus = 'IKKE_UTFØRT'"""
    )
    fun finnBehandlingerSentTilGodkjenning(fagsakId: Long): List<Behandling>

    @Query("SELECT b FROM Behandling b JOIN b.fagsak f WHERE f.id = :fagsakId AND b.status = 'AVSLUTTET' AND f.arkivert = false")
    fun findByFagsakAndAvsluttet(fagsakId: Long): List<Behandling>

    @Lock(LockModeType.NONE)
    @Query("SELECT count(*) FROM Behandling b JOIN b.fagsak f WHERE NOT b.status = 'AVSLUTTET' AND f.arkivert = false")
    fun finnAntallBehandlingerIkkeAvsluttet(): Long

    @Lock(LockModeType.NONE)
    @Query("SELECT b.opprettetTidspunkt FROM Behandling b JOIN b.fagsak f WHERE NOT b.status = 'AVSLUTTET' AND f.arkivert = false")
    fun finnOpprettelsestidspunktPåÅpneBehandlinger(): List<LocalDateTime>

    @Lock(LockModeType.NONE)
    @Query("SELECT b FROM Behandling b JOIN b.fagsak f WHERE b.opprettetTidspunkt < :opprettetFør AND b.status <> 'AVSLUTTET' AND f.arkivert = false")
    fun finnÅpneBehandlinger(opprettetFør: LocalDateTime): List<Behandling>

    @Query("SELECT DISTINCT aty.behandlingId FROM AndelTilkjentYtelse aty WHERE aty.behandlingId in :iverksatteLøpende AND aty.sats = :gammelSats AND aty.kalkulertUtbetalingsbeløp > 0 AND aty.stønadTom >= :månedÅrForEndring")
    fun finnBehandlingerForSatsendring(
        iverksatteLøpende: List<Long>,
        gammelSats: Int,
        månedÅrForEndring: YearMonth
    ): List<Long>

    @Query("SELECT new kotlin.Pair(b.opprettetÅrsak, count(*)) from Behandling b group by b.opprettetÅrsak")
    fun finnAntallBehandlingerPerÅrsak(): List<Pair<BehandlingÅrsak, Long>>

    @Query("SELECT b.id from Behandling b where b.opprettetÅrsak in (:opprettetÅrsak)")
    fun finnBehandlingIdMedOpprettetÅrsak(opprettetÅrsak: List<BehandlingÅrsak>): List<Long>
}
