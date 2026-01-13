package no.nav.familie.ba.sak.kjerne.fagsak

import jakarta.persistence.LockModeType
import no.nav.familie.ba.sak.internal.FagsakMedFlereMigreringer
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
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
    fun finnFagsakForAktør(
        aktør: Aktør,
        type: FagsakType = FagsakType.NORMAL,
    ): Fagsak?

    @Lock(LockModeType.NONE)
    @Query(value = "SELECT f FROM Fagsak f WHERE f.aktør = :aktør and f.type = 'INSTITUSJON' and f.arkivert = false and f.institusjon.orgNummer = :orgNummer")
    fun finnFagsakForInstitusjonOgOrgnummer(
        aktør: Aktør,
        orgNummer: String,
    ): Fagsak?

    @Lock(LockModeType.NONE)
    @Query(value = "SELECT f FROM Fagsak f WHERE f.aktør = :barnAktør and f.type = 'SKJERMET_BARN' and f.arkivert = false and f.skjermetBarnSøker.aktør = :søkerAktør")
    fun finnFagsakForSkjermetBarnSøker(
        barnAktør: Aktør,
        søkerAktør: Aktør,
    ): Fagsak?

    @Lock(LockModeType.NONE)
    @Query(value = "SELECT f FROM Fagsak f WHERE f.aktør = :aktør and f.arkivert = false")
    fun finnFagsakerForAktør(aktør: Aktør): List<Fagsak>

    @Lock(LockModeType.NONE)
    @Query(value = "SELECT f from Fagsak f WHERE f.status = 'LØPENDE'  AND f.arkivert = false")
    fun finnLøpendeFagsaker(): List<Fagsak>

    @Lock(LockModeType.NONE)
    @Query(value = "SELECT f.id from Fagsak f WHERE f.status = 'LØPENDE'  AND f.arkivert = false")
    fun finnIdPåLøpendeFagsaker(): List<Long>

    @Lock(LockModeType.NONE)
    @Query(value = "SELECT f.id from Fagsak f WHERE f.status = 'LØPENDE'  AND f.arkivert = false")
    fun finnLøpendeFagsaker(page: Pageable): Slice<Long>

    @Query(
        value = """SELECT f.id
            FROM   Fagsak f
            WHERE  NOT EXISTS (
                    SELECT 1
                    FROM   satskjoering
                    WHERE  fk_fagsak_id = f.id
                    AND sats_tid  = :satsTidspunkt
                ) AND f.status = 'LØPENDE' AND f.arkivert = false""",
        nativeQuery = true,
    )
    fun finnLøpendeFagsakerForSatsendring(
        satsTidspunkt: LocalDate,
        page: Pageable,
    ): Page<Long>

    @Query(
        value = """SELECT distinct f.institusjon.orgNummer
            FROM   Fagsak f
            WHERE  f.status = 'LØPENDE' 
            AND f.institusjon is not null
            AND f.arkivert = false""",
        nativeQuery = false,
    )
    fun finnOrgnummerForLøpendeFagsaker(): List<String>

    @Query(
        value = """
            WITH sisteiverksatte AS (
                SELECT DISTINCT ON (b.fk_fagsak_id) b.id, b.fk_fagsak_id, stonad_tom
                 FROM behandling b
                          INNER JOIN tilkjent_ytelse ty ON b.id = ty.fk_behandling_id
                          INNER JOIN fagsak f ON f.id = b.fk_fagsak_id
                 WHERE f.status = 'LØPENDE'
                   AND f.arkivert = FALSE
                   AND b.status = 'AVSLUTTET'
                   AND b.resultat != 'AVSLÅTT'
                   AND b.resultat NOT LIKE '%HENLAGT%'
                 ORDER BY b.fk_fagsak_id, b.aktivert_tid DESC
             )
            
            SELECT silp.fk_fagsak_id
            FROM sisteiverksatte silp
            WHERE silp.stonad_tom < DATE_TRUNC('month', NOW())
               OR NOT EXISTS (
                SELECT 1
                FROM andel_tilkjent_ytelse aty
                WHERE aty.fk_behandling_id = silp.id
                  AND aty.stonad_tom >= DATE_TRUNC('month', NOW())
                  AND aty.prosent > 0
                );
                """,
        nativeQuery = true,
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
                    SELECT p.personopplysningGrunnlag.id FROM Person p 
                    WHERE p.fødselsdato BETWEEN :fom AND :tom 
                    AND p.type = 'BARN'
                )
            )
        )
        """,
    )
    fun finnLøpendeFagsakMedBarnMedFødselsdatoInnenfor(
        fom: LocalDate,
        tom: LocalDate,
    ): Set<Fagsak>

    @Lock(LockModeType.NONE)
    @Query(value = "SELECT count(*) from Fagsak where arkivert = false")
    fun finnAntallFagsakerTotalt(): Long

    @Query(
        value = """
            SELECT f.id 
            from Fagsak f where f.arkivert = false
            order by f.id asc
            """,
    )
    fun hentFagsakerSomIkkeErArkivert(page: Pageable): Slice<Long>

    @Lock(LockModeType.NONE)
    @Query(value = "SELECT count(*) from Fagsak f where f.status='LØPENDE' and f.arkivert = false")
    fun finnAntallFagsakerLøpende(): Long

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
        """,
    )
    fun finnAlleFagsakerMedOpphørSmåbarnstilleggIMåned(
        iverksatteLøpendeBehandlinger: List<Long>,
        stønadTom: YearMonth = YearMonth.now().minusMonths(1),
        innværendeMåned: YearMonth = YearMonth.now(),
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
        """,
    )
    fun finnAlleFagsakerMedOppstartSmåbarnstilleggIMåned(
        iverksatteLøpendeBehandlinger: List<Long>,
        stønadFom: YearMonth = YearMonth.now(),
    ): List<Long>

    @Query(
        """
        SELECT distinct f from Fagsak f
         JOIN Behandling b ON b.fagsak.id = f.id
         JOIN AndelTilkjentYtelse aty ON aty.behandlingId = b.id
        WHERE aty.aktør = :aktør
        """,
    )
    fun finnFagsakerSomHarAndelerForAktør(aktør: Aktør): List<Fagsak>

    @Query(
        """
        WITH fagsakerMigrertFlereGanger AS (SELECT f.id
                                    FROM Fagsak f
                                             JOIN Behandling b ON f.id = b.fk_fagsak_id
                                             join aktoer a on f.fk_aktoer_id = a.aktoer_id
                                    WHERE f.status = 'LØPENDE'
                                      AND b.opprettet_aarsak in ('HELMANUELL_MIGRERING', 'MIGRERING')
                                      AND b.resultat NOT IN (
                                                             'HENLAGT_FEILAKTIG_OPPRETTET',
                                                             'HENLAGT_SØKNAD_TRUKKET',
                                                             'HENLAGT_AUTOMATISK_FØDSELSHENDELSE',
                                                             'HENLAGT_AUTOMATISK_SMÅBARNSTILLEGG',
                                                             'HENLAGT_TEKNISK_VEDLIKEHOLD'
                                        )
                                      AND b.status = 'AVSLUTTET'
                                    GROUP BY f.id
                                    HAVING COUNT(*) >= 2)

        SELECT b.fk_fagsak_id as fagsakId, p.foedselsnummer as fødselsnummer
        FROM Behandling b
                 JOIN fagsakerMigrertFlereGanger fmfg ON b.fk_fagsak_id = fmfg.id
                 JOIN vedtak v ON b.id = v.fk_behandling_id
                 JOIN fagsak f ON f.id = b.fk_fagsak_id
                 join personident p ON p.fk_aktoer_id = f.fk_aktoer_id
        WHERE b.opprettet_aarsak in ('HELMANUELL_MIGRERING', 'MIGRERING')
          AND b.resultat NOT IN (
                                 'HENLAGT_FEILAKTIG_OPPRETTET',
                                 'HENLAGT_SØKNAD_TRUKKET',
                                 'HENLAGT_AUTOMATISK_FØDSELSHENDELSE',
                                 'HENLAGT_AUTOMATISK_SMÅBARNSTILLEGG'
                                 'HENLAGT_TEKNISK_VEDLIKEHOLD'
            )
          AND vedtaksdato > :month
        """,
        nativeQuery = true,
    )
    fun finnFagsakerMedFlereMigreringsbehandlinger(month: LocalDateTime): List<FagsakMedFlereMigreringer>

    @Query(
        """
        WITH siste_iverksatte_behandling_for_løpende_fagsaker AS (
            SELECT DISTINCT ON (b.fk_fagsak_id) b.id
            FROM behandling b
                INNER JOIN fagsak f ON f.id = b.fk_fagsak_id
                INNER JOIN tilkjent_ytelse ty ON b.id = ty.fk_behandling_id
            WHERE f.status = 'LØPENDE'
            AND ty.utbetalingsoppdrag IS NOT NULL
            AND f.arkivert = false
            ORDER BY b.fk_fagsak_id, b.aktivert_tid DESC
        )
        SELECT DISTINCT personident.foedselsnummer
        FROM siste_iverksatte_behandling_for_løpende_fagsaker b
            INNER JOIN gr_personopplysninger po ON b.id = po.fk_behandling_id
            INNER JOIN po_person p ON po.id = p.fk_gr_personopplysninger_id
            INNER JOIN personident ON personident.fk_aktoer_id = p.fk_aktoer_id
        WHERE personident.aktiv = true
        """,
        nativeQuery = true,
    )
    fun finnIdenterForLøpendeFagsaker(): List<String>
}
