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

    @Query(
        """
            SELECT aty.id               AS id,
                   p.foedselsnummer     AS ident,
                   aty.stonad_fom       AS fom,
                   aty.stonad_tom       AS tom,
                   aty.prosent          AS prosent,
                   ty.endret_dato       AS endretdato,
                   aty.fk_behandling_id AS behandlingid
            FROM andel_tilkjent_ytelse aty
                     INNER JOIN
                 tilkjent_ytelse ty ON aty.tilkjent_ytelse_id = ty.id
                     INNER JOIN
                 personident p ON aty.fk_aktoer_id = p.fk_aktoer_id
            WHERE aty.tilkjent_ytelse_id IN (
                SELECT MAX(ty.id)
                FROM andel_tilkjent_ytelse aty
                         INNER JOIN
                     tilkjent_ytelse ty ON aty.tilkjent_ytelse_id = ty.id
                         INNER JOIN
                     personident p ON aty.fk_aktoer_id = p.fk_aktoer_id
                WHERE p.foedselsnummer IN :personIdenter
                  AND ty.utbetalingsoppdrag IS NOT NULL
                GROUP BY p.foedselsnummer
            )
              AND aty.type = 'UTVIDET_BARNETRYGD'
              AND aty.stonad_fom <= :tom
              AND aty.stonad_tom >= :fom
        """,
        nativeQuery = true
    )
    @Timed
    fun finnPerioderMedUtvidetBarnetrygdForPersoner(
        personIdenter: List<String>,
        fom: LocalDateTime,
        tom: LocalDateTime
    ): List<AndelTilkjentYtelsePeriode>
}
