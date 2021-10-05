package no.nav.familie.ba.sak.kjerne.fagsak

import io.micrometer.core.annotation.Timed
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.ekstern.skatteetaten.AndelTilkjentYtelsePeriode
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime
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
    @Query(value = "SELECT f FROM Fagsak f, FagsakPerson fp WHERE f.id = fp.fagsak.id and fp.personIdent = :personIdent and f.arkivert = false")
    fun finnFagsakForPersonIdent(personIdent: PersonIdent): Fagsak?

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
        AND f.status <> 'OPPRETTET' 
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

    @Query(value = """WITH qualified AS (
    SELECT *
    FROM ((
              SELECT aty.person_ident       ident,
                     aty.stonad_fom         fom,
                     aty.stonad_tom         tom,
                     aty.prosent            prosent,
                     aty.tilkjent_ytelse_id aty_tyid,
                     aty.id
              FROM andel_tilkjent_ytelse aty
              WHERE aty.type = 'UTVIDET_BARNETRYGD'
                AND aty.person_ident IN :personIdenter
                AND aty.stonad_fom <= :tom
                AND aty.stonad_tom >= :fom
          ) AS qualified_aty
             INNER JOIN (
        SELECT ty.id tyid, ty.endret_dato endret_dato
        FROM tilkjent_ytelse ty
        WHERE ty.utbetalingsoppdrag IS NOT NULL
    ) AS qualified_ty
                        ON qualified_aty.aty_tyid = qualified_ty.tyid)
)

SELECT qualified.id             AS id,
       qualified.ident          AS ident,
       qualified.prosent        AS prosent,
       qualified.endret_dato AS endretDato,
       qualified.fom            AS fom,
       qualified.tom            AS tom
FROM (SELECT ident, MAX(endret_dato) dato
      FROM qualified
      GROUP BY ident
     ) AS latest
         INNER JOIN qualified
                    ON qualified.ident = latest.ident AND qualified.endret_dato = latest.dato
""", nativeQuery = true)
    @Timed
    fun finnStonadPeriodMedUtvidetBarnetrygdForPersoner(
        personIdenter: List<String>,
        fom: LocalDateTime,
        tom: LocalDateTime
    ): List<AndelTilkjentYtelsePeriode>
}