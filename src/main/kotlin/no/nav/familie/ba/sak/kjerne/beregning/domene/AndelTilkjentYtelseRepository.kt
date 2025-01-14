package no.nav.familie.ba.sak.kjerne.beregning.domene

import io.micrometer.core.annotation.Timed
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.YearMonth

interface AndelTilkjentYtelseRepository : JpaRepository<AndelTilkjentYtelse, Long> {
    @Query(value = "SELECT aty FROM AndelTilkjentYtelse aty WHERE aty.behandlingId IN :behandlingIder")
    fun finnAndelerTilkjentYtelseForBehandlinger(behandlingIder: List<Long>): List<AndelTilkjentYtelse>

    @Query(value = "SELECT aty FROM AndelTilkjentYtelse aty WHERE aty.behandlingId = :behandlingId")
    fun finnAndelerTilkjentYtelseForBehandling(behandlingId: Long): List<AndelTilkjentYtelse>

    @Query(value = "SELECT aty FROM AndelTilkjentYtelse aty WHERE aty.behandlingId = :behandlingId AND aty.aktør = :barnAktør")
    fun finnAndelerTilkjentYtelseForBehandlingOgBarn(
        behandlingId: Long,
        barnAktør: Aktør,
    ): List<AndelTilkjentYtelse>

    @Query(value = "SELECT aty from AndelTilkjentYtelse aty WHERE aty.aktør = :aktør")
    fun finnAndelerTilkjentYtelseForAktør(aktør: Aktør): List<AndelTilkjentYtelse>

    @Query(value = "SELECT aty FROM AndelTilkjentYtelse aty WHERE aty.behandlingId IN :behandlingIder AND aty.stønadTom >= :avstemmingstidspunkt")
    fun finnLøpendeAndelerTilkjentYtelseForBehandlinger(
        behandlingIder: List<Long>,
        avstemmingstidspunkt: YearMonth,
    ): List<AndelTilkjentYtelse>

    @Query(
        """
            SELECT DISTINCT p.foedselsnummer AS ident
            FROM andel_tilkjent_ytelse aty
                     INNER JOIN tilkjent_ytelse ty ON aty.fk_behandling_id = ty.fk_behandling_id
                     INNER JOIN behandling b ON aty.fk_behandling_id = b.id
                     INNER JOIN fagsak f ON b.fk_fagsak_id = f.id
                     INNER JOIN personident p ON f.fk_aktoer_id = p.fk_aktoer_id
            WHERE p.aktiv = true
              AND ty.utbetalingsoppdrag is not null
              AND EXTRACT('Year' FROM aty.stonad_fom) <= CAST(:år AS INTEGER )
              AND EXTRACT('Year' FROM aty.stonad_tom) >= CAST(:år AS INTEGER );
        """,
        nativeQuery = true,
    )
    @Timed
    fun finnIdenterMedLøpendeBarnetrygdForGittÅr(
        år: Int,
    ): List<String>

    @Query(
        """
        WITH andeler AS (
            SELECT
             aty.id,
             row_number() OVER (PARTITION BY aty.type, aty.fk_aktoer_id ORDER BY aty.periode_offset DESC, b.aktivert_tid ASC) rn
             FROM andel_tilkjent_ytelse aty
              JOIN tilkjent_ytelse ty ON ty.id = aty.tilkjent_ytelse_id
              JOIN Behandling b ON b.id = aty.fk_behandling_id
             WHERE b.fk_fagsak_id = :fagsakId
               AND ty.utbetalingsoppdrag IS NOT NULL
               AND json_extract_path_text(cast(ty.utbetalingsoppdrag as json), 'utbetalingsperiode') != '[]'
               AND aty.periode_offset IS NOT NULL
               AND b.status = 'AVSLUTTET')
        SELECT aty.* FROM andel_tilkjent_ytelse aty WHERE id IN (SELECT id FROM andeler WHERE rn = 1)
    """,
        nativeQuery = true,
    )
    fun hentSisteAndelPerIdentOgType(fagsakId: Long): List<AndelTilkjentYtelse>
}
