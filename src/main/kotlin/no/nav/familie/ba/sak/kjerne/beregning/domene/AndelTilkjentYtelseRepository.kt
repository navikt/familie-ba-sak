package no.nav.familie.ba.sak.kjerne.beregning.domene

import io.micrometer.core.annotation.Timed
import no.nav.familie.ba.sak.ekstern.skatteetaten.AndelTilkjentYtelsePeriode
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime
import java.time.YearMonth

interface AndelTilkjentYtelseRepository : JpaRepository<AndelTilkjentYtelse, Long> {

    @Query(value = "SELECT aty FROM AndelTilkjentYtelse aty WHERE aty.behandlingId IN :behandlingIder")
    fun finnAndelerTilkjentYtelseForBehandlinger(behandlingIder: List<Long>): List<AndelTilkjentYtelse>

    @Query(value = "SELECT aty FROM AndelTilkjentYtelse aty WHERE aty.behandlingId = :behandlingId")
    fun finnAndelerTilkjentYtelseForBehandling(behandlingId: Long): List<AndelTilkjentYtelse>

    @Query(value = "SELECT aty FROM AndelTilkjentYtelse aty WHERE aty.behandlingId = :behandlingId AND aty.aktør = :barnAktør")
    fun finnAndelerTilkjentYtelseForBehandlingOgBarn(behandlingId: Long, barnAktør: Aktør): List<AndelTilkjentYtelse>

    @Query(value = "SELECT aty FROM AndelTilkjentYtelse aty WHERE aty.behandlingId IN :behandlingIder AND aty.stønadTom >= :avstemmingstidspunkt")
    fun finnLøpendeAndelerTilkjentYtelseForBehandlinger(
        behandlingIder: List<Long>,
        avstemmingstidspunkt: YearMonth
    ): List<AndelTilkjentYtelse>

    // The query only returns the periods that overlap with the given year. Any periods that are
    // totally out of the year scope will be ignored.
    @Query(
        value = """WITH qualified AS (
    SELECT *
    FROM ((
              SELECT personident.foedselsnummer       ident,
                     aty.stonad_fom                     fom,
                     aty.stonad_tom                     tom,
                     aty.prosent                    prosent,
                     aty.tilkjent_ytelse_id         aty_tyid,
                     aty.id, 
                     aty.fk_behandling_id           behandling_id   
              FROM andel_tilkjent_ytelse aty
              JOIN personident personident on personident.fk_aktoer_id = aty.fk_aktoer_id
              WHERE aty.type = 'UTVIDET_BARNETRYGD'
                AND personident.foedselsnummer IN :personIdenter
                AND personident.aktiv = true
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
       qualified.tom            AS tom,
       qualified.behandling_id  AS behandlingId
FROM (SELECT ident, MAX(endret_dato) dato
      FROM qualified
      GROUP BY ident
     ) AS latest
         INNER JOIN qualified
                    ON qualified.ident = latest.ident AND qualified.endret_dato = latest.dato
""",
        nativeQuery = true
    )
    @Timed
    fun finnStonadPeriodMedUtvidetBarnetrygdForPersoner(
        personIdenter: List<String>,
        fom: LocalDateTime,
        tom: LocalDateTime
    ): List<AndelTilkjentYtelsePeriode>
}
