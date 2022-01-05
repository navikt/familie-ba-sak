package no.nav.familie.ba.sak.kjerne.fagsak

import io.micrometer.core.annotation.Timed
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.YearMonth
import java.util.Optional
import javax.persistence.LockModeType

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
    @Query(value = "SELECT f FROM Fagsak f WHERE f.aktør = :aktør and f.arkivert = false")
    fun finnFagsakForAktør(aktør: Aktør): Fagsak?

    @Lock(LockModeType.NONE)
    @Query(value = "SELECT f from Fagsak f WHERE f.status = 'LØPENDE'  AND f.arkivert = false")
    fun finnLøpendeFagsaker(): List<Fagsak>

    @Modifying
    @Query(
        value = """select id from fagsak
                        where fagsak.id in (
                            with sisteIverksatte as (
                                select b.fk_fagsak_id as fagsakId, max(b.id) as behandlingId
                                from behandling b
                                         inner join tilkjent_ytelse ty on b.id = ty.fk_behandling_id
                                         inner join fagsak f on f.id = b.fk_fagsak_id
                                where ty.utbetalingsoppdrag IS NOT NULL
                                  and f.status = 'LØPENDE'
                                  and f.arkivert = false
                                group by b.id)
                            select sisteIverksatte.fagsakId
                            from sisteIverksatte
                                     inner join tilkjent_ytelse ty on sisteIverksatte.behandlingId = ty.fk_behandling_id
                            where ty.stonad_tom < date_trunc('month', now()))""",
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

    @Lock(LockModeType.NONE)
    @Query(value = "SELECT count(*) from Fagsak f where f.status='LØPENDE' and f.arkivert = false")
    fun finnAntallFagsakerLøpende(): Long

    @Lock(LockModeType.NONE)
    @Query(
        value = """
        SELECT new kotlin.Pair(f , MAX(ty.opprettetDato))
        FROM Behandling b
               INNER JOIN Fagsak f ON f.id = b.fagsak.id
               INNER JOIN TilkjentYtelse ty ON b.id = ty.behandling.id
        WHERE ty.utbetalingsoppdrag IS NOT NULL
        AND EXISTS(
            SELECT aty.type FROM AndelTilkjentYtelse aty
            WHERE aty.tilkjentYtelse.id = ty.id
            AND aty.type = 'UTVIDET_BARNETRYGD'
            AND aty.stønadFom <= :tom
            AND aty.stønadTom >= :fom
        )
        GROUP BY f.id
    """
    )
    @Timed
    fun finnFagsakerMedUtvidetBarnetrygdInnenfor(fom: YearMonth, tom: YearMonth): List<Pair<Fagsak, LocalDate>>

    @Query(
        value = """
            SELECT DISTINCT ON (b.fk_fagsak_id) b.fk_fagsak_id
            FROM andel_tilkjent_ytelse aty
                JOIN behandling b ON aty.fk_behandling_id = b.id
                JOIN tilkjent_ytelse ty ON b.id = ty.fk_behandling_id
            WHERE
                    aty.type = 'SMÅBARNSTILLEGG'
                AND b.status = 'AVSLUTTET'
                AND date_trunc('month', aty.stonad_tom) = date_trunc('month', 'date' :opphørsmåned)
                AND ty.utbetalingsoppdrag IS NOT NULL
            ORDER BY b.fk_fagsak_id, b.opprettet_tid DESC
        """,
        nativeQuery = true
    )
    fun finnAlleFagsakerMedOpphørSmåbarnstilleggIMåned(
        opphørsmåned: YearMonth,
    ): List<Long>
}
