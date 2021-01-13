package no.nav.familie.ba.sak.beregning.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.YearMonth

interface AndelTilkjentYtelseRepository : JpaRepository<AndelTilkjentYtelse, Long> {

    @Query(value = "SELECT aty FROM AndelTilkjentYtelse aty WHERE aty.behandlingId IN :behandlingIder")
    fun finnAndelerTilkjentYtelseForBehandlinger(behandlingIder: List<Long>): List<AndelTilkjentYtelse>

    @Query(value = "SELECT aty FROM AndelTilkjentYtelse aty WHERE aty.behandlingId IN :behandlingIder AND aty.stønadTom >= :filtreringsMåned")
    fun finnAndelerTilkjentYtelseForBehandlingerFørMåned(behandlingIder: List<Long>,
                                                         filtreringsMåned: YearMonth): List<AndelTilkjentYtelse>

    @Modifying
    @Query(value = "DELETE FROM AndelTilkjentYtelse aty WHERE aty.behandlingId = :behandlingId")
    fun slettAlleAndelerTilkjentYtelseForBehandling(behandlingId: Long)
}