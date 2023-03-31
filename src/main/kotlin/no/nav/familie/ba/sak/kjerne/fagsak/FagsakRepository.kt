package no.nav.familie.ba.sak.kjerne.fagsak

import io.micrometer.core.annotation.Timed
import jakarta.persistence.LockModeType
import no.nav.familie.ba.sak.ekstern.skatteetaten.UtvidetSkatt
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.Optional

@Repository
interface FagsakRepository : JpaRepository<Fagsak, Long> {

    @Lock(LockModeType.PESSIMISTIC_FORCE_INCREMENT)
    fun save(fagsak: Fagsak): Fagsak

    @Lock(LockModeType.NONE)
    override fun findById(id: Long): Optional<Fagsak>

    @Lock(LockModeType.NONE)
    @Query(value = "SELECT f FROM Fagsak f WHERE f.id = :fagsakId AND f.arkivert = false")
    fun finnFagsak(fagsakId: Long): Fagsak?

    @Lock(LockModeType.NONE)
    @Query(value = "SELECT f FROM Fagsak f WHERE f.aktør = :aktør and f.type = :type and f.arkivert = false")
    fun finnFagsakForAktør(aktør: Aktør, type: FagsakType = FagsakType.NORMAL): Fagsak?

    @Lock(LockModeType.NONE)
    @Query(value = "SELECT f FROM Fagsak f WHERE f.aktør = :aktør and f.type = 'INSTITUSJON' and f.status <> 'AVSLUTTET' and f.arkivert = false and f.institusjon.orgNummer = :orgNummer")
    fun finnFagsakForInstitusjonOgOrgnummer(aktør: Aktør, orgNummer: String): Fagsak?

    @Lock(LockModeType.NONE)
    @Query(value = "SELECT f FROM Fagsak f WHERE f.aktør = :aktør and f.arkivert = false")
    fun finnFagsakerForAktør(aktør: Aktør): List<Fagsak>

    @Lock(LockModeType.NONE)
    @Query(value = "SELECT f from Fagsak f WHERE f.status = 'LØPENDE'  AND f.arkivert = false")
    fun finnLøpendeFagsaker(): List<Fagsak>

    @Query(
        value = """SELECT f.*
            FROM   Fagsak f
            WHERE  NOT EXISTS (
                    SELECT  -- SELECT list mostly irrelevant; can just be empty in Postgres
                    FROM   satskjoering
                    WHERE  fk_fagsak_id = f.id
                ) AND f.status = 'LØPENDE' AND f.arkivert = false""",
        nativeQuery = true
    )
    fun finnLøpendeFagsakerForSatsendring(page: Pageable): Page<Fagsak>

    @Modifying
    @Query(
        value = """SELECT id FROM fagsak
                        WHERE fagsak.id IN (
                            WITH sisteiverksatte AS (
                                SELECT b.fk_fagsak_id AS fagsakid, MAX(b.opprettet_tid) AS opprettet_tid
                                FROM behandling b
                                         INNER JOIN tilkjent_ytelse ty ON b.id = ty.fk_behandling_id
                                         INNER JOIN fagsak f ON f.id = b.fk_fagsak_id
                                WHERE ty.utbetalingsoppdrag IS NOT NULL
                                  AND f.status = 'LØPENDE'
                                  AND f.arkivert = FALSE
                                GROUP BY b.fk_fagsak_id)
                                
                            SELECT silp.fagsakid
                            FROM sisteiverksatte silp
                                     INNER JOIN behandling b ON b.fk_fagsak_id = silp.fagsakid
                                     INNER JOIN tilkjent_ytelse ty ON b.id = ty.fk_behandling_id
                            WHERE b.opprettet_tid = silp.opprettet_tid AND ty.stonad_tom < DATE_TRUNC('month', NOW()))""",
        nativeQuery = true
    )
    fun finnFagsakerSomSkalAvsluttes(): List<Long>

    /**
     * Denne skal plukke fagsaker som løper _og_ har barn født innenfor anngitt tidsintervall.
     * Brukes til å sende ut automatiske brev ved reduksjon 6 og 18 år blant annet.
     * Ved 18 år og dersom hele fagsaken opphører så skal det ikke sendes ut brev og derfor sjekker
     * vi kun løpende fagsaker.
     */
    @Query(
        value = """
        SELECT f FROM Fagsak f
        WHERE f.arkivert = false AND f.status = 'LØPENDE' AND f IN ( 
            SELECT b.fagsak FROM Behandling b 
            WHERE b.aktiv=true AND b.id IN (
                SELECT pg.behandlingId FROM PersonopplysningGrunnlag pg
                WHERE pg.aktiv=true AND pg.id IN (
                    SELECT p.personopplysningGrunnlag FROM Person p 
                    WHERE p.fødselsdato BETWEEN :fom AND :tom 
                    AND p.type = 'BARN'
                )
            )
        )
        """
    )
    fun finnLøpendeFagsakMedBarnMedFødselsdatoInnenfor(fom: LocalDate, tom: LocalDate): Set<Fagsak>

    @Lock(LockModeType.NONE)
    @Query(value = "SELECT count(*) from Fagsak where arkivert = false")
    fun finnAntallFagsakerTotalt(): Long

    @Query(value = "SELECT f from Fagsak f where f.arkivert = false")
    fun hentFagsakerSomIkkeErArkivert(): List<Fagsak>

    @Lock(LockModeType.NONE)
    @Query(value = "SELECT count(*) from Fagsak f where f.status='LØPENDE' and f.arkivert = false")
    fun finnAntallFagsakerLøpende(): Long

    @Query(
        value = """
        SELECT p.foedselsnummer AS fnr,
               MAX(ty.endret_dato) AS sistevedtaksdato
        FROM andel_tilkjent_ytelse aty
                 INNER JOIN
             tilkjent_ytelse ty ON aty.tilkjent_ytelse_id = ty.id
                 INNER JOIN personident p ON aty.fk_aktoer_id = p.fk_aktoer_id
        WHERE ty.utbetalingsoppdrag IS NOT NULL
          AND aty.type = 'UTVIDET_BARNETRYGD'
          AND aty.stonad_fom <= :tom
          AND aty.stonad_tom >= :fom
          AND p.aktiv = TRUE
        GROUP BY p.foedselsnummer
    """,
        nativeQuery = true
    )
    @Timed
    fun finnFagsakerMedUtvidetBarnetrygdInnenfor(fom: LocalDateTime, tom: LocalDateTime): List<UtvidetSkatt>

    @Query(
        """
            SELECT DISTINCT b.fagsak.id
            FROM AndelTilkjentYtelse aty
                JOIN Behandling b ON b.id = aty.behandlingId
                JOIN TilkjentYtelse ty ON b.id = ty.behandling.id
            WHERE
                    b.id in :iverksatteLøpendeBehandlinger
                AND NOT EXISTS (SELECT b2 from Behandling b2 where b2.fagsak.id = b.fagsak.id AND b2.status <> 'AVSLUTTET')
                AND NOT EXISTS (SELECT aty2 from AndelTilkjentYtelse aty2 where aty2.behandlingId = b.id AND aty2.type = 'SMÅBARNSTILLEGG' AND aty.stønadFom = :innværendeMåned)
                AND aty.type = 'SMÅBARNSTILLEGG'
                AND aty.stønadTom = :stønadTom
        """
    )
    fun finnAlleFagsakerMedOpphørSmåbarnstilleggIMåned(
        iverksatteLøpendeBehandlinger: List<Long>,
        stønadTom: YearMonth = YearMonth.now().minusMonths(1),
        innværendeMåned: YearMonth = YearMonth.now()
    ): List<Long>

    @Query(
        """
            SELECT DISTINCT b.fagsak.id
            FROM AndelTilkjentYtelse aty
                JOIN Behandling b ON b.id = aty.behandlingId
                JOIN TilkjentYtelse ty ON b.id = ty.behandling.id
            WHERE
                    b.id in :iverksatteLøpendeBehandlinger
                AND NOT EXISTS (SELECT b2 from Behandling b2 where b2.fagsak.id = b.fagsak.id AND b2.status <> 'AVSLUTTET')
                AND aty.type = 'SMÅBARNSTILLEGG'
                AND aty.stønadFom = :stønadFom
        """
    )
    fun finnAlleFagsakerMedOppstartSmåbarnstilleggIMåned(
        iverksatteLøpendeBehandlinger: List<Long>,
        stønadFom: YearMonth = YearMonth.now()
    ): List<Long>
}
