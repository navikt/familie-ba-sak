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
                            where ty.stonad_tom < now())""",
        nativeQuery = true
    )
    fun finnFagsakerSomSkalAvsluttes(): List<Long>

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

    @Lock(LockModeType.NONE)
    @Query(
        value = """
        SELECT f FROM Fagsak f
        WHERE f.arkivert = false AND f.status = 'LØPENDE' AND f IN ( 
            SELECT b.fagsak FROM Behandling b 
            WHERE b.aktiv=true AND b.id IN (
                SELECT pog.behandlingId FROM PeriodeOvergangsstønadGrunnlag pog
                WHERE pog.tom BETWEEN :fom AND :tom
            )
        )
        """
    )
    fun finnLøpendeFagsakerMedOpphørAvFullOvergangsstonadIInterval(fom: LocalDate, tom: LocalDate): Set<Fagsak>
}
