package no.nav.familie.ba.sak.kjerne.fagsak

import io.micrometer.core.annotation.Timed
import no.nav.familie.ba.sak.ekstern.skatteetaten.UtvidetSkatt
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakEier.OMSORGSPERSON
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
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
    @Query(value = "SELECT f FROM Fagsak f WHERE f.aktør = :aktør and f.eier = :eier and f.arkivert = false")
    fun finnFagsakForAktør(aktør: Aktør, eier: FagsakEier = OMSORGSPERSON): Fagsak?

    @Lock(LockModeType.NONE)
    @Query(value = "SELECT f from Fagsak f WHERE f.status = 'LØPENDE'  AND f.arkivert = false")
    fun finnLøpendeFagsaker(): List<Fagsak>

    @Modifying
    @Query(
        value = """select id from fagsak
                        where fagsak.id in (
                            with sisteIverksatte as (
                                select b.fk_fagsak_id as fagsakId, max(b.opprettet_tid) as opprettet_tid
                                from behandling b
                                         inner join tilkjent_ytelse ty on b.id = ty.fk_behandling_id
                                         inner join fagsak f on f.id = b.fk_fagsak_id
                                where ty.utbetalingsoppdrag IS NOT NULL
                                  and f.status = 'LØPENDE'
                                  and f.arkivert = false
                                group by b.id)
                                
                            select silp.fagsakId
                            from sisteIverksatte silp
                                     inner join behandling b on b.fk_fagsak_id = silp.fagsakId
                                     inner join tilkjent_ytelse ty on b.id = ty.fk_behandling_id
                            where b.opprettet_tid = silp.opprettet_tid and ty.stonad_tom < date_trunc('month', now()))""",
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

    @Query(
        value = """
        SELECT p.foedselsnummer as fnr,
               MAX(ty.endret_dato) as sisteVedtaksdato
        FROM andel_tilkjent_ytelse aty
                 INNER JOIN
             tilkjent_ytelse ty ON aty.tilkjent_ytelse_id = ty.id
                 INNER JOIN personident p on aty.fk_aktoer_id = p.fk_aktoer_id
        WHERE ty.utbetalingsoppdrag is not null
          AND aty.type = 'UTVIDET_BARNETRYGD'
          AND aty.stonad_fom <= :tom
          AND aty.stonad_tom >= :fom
          AND p.aktiv = true
        group by p.foedselsnummer
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
                AND aty.type = 'SMÅBARNSTILLEGG'
                AND aty.stønadTom = :stønadTom
        """,
    )
    fun finnAlleFagsakerMedOpphørSmåbarnstilleggIMåned(
        iverksatteLøpendeBehandlinger: List<Long>,
        stønadTom: YearMonth = YearMonth.now().minusMonths(1),
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
