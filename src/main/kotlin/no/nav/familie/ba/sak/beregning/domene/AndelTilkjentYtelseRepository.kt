package no.nav.familie.ba.sak.beregning.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface AndelTilkjentYtelseRepository : JpaRepository<AndelTilkjentYtelse, Long> {

    @Query(value = "SELECT aty FROM AndelTilkjentYtelse aty WHERE aty.behandlingId IN :behandlingIder AND aty.aktiv = true")
    fun finnAndelerTilkjentYtelseForBehandlinger(behandlingIder: List<Long>): List<AndelTilkjentYtelse>

    @Query(value = "SELECT aty FROM AndelTilkjentYtelse aty WHERE aty.behandlingId = :behandlingId AND aty.aktiv = true")
    fun finnAndelerTilkjentYtelseForBehandling(behandlingId: Long): List<AndelTilkjentYtelse>

    @Query(value = "SELECT aty FROM AndelTilkjentYtelse aty WHERE aty.behandlingId IN :behandlingIder AND aty.stønadTom >= CURRENT_TIMESTAMP AND aty.aktiv = true")
    fun finnLøpendeAndelerTilkjentYtelseForBehandlinger(behandlingIder: List<Long>): List<AndelTilkjentYtelse>

    @Modifying
    @Query(value = "DELETE FROM AndelTilkjentYtelse aty WHERE aty.behandlingId = :behandlingId")
    fun slettAlleAndelerTilkjentYtelseForBehandling(behandlingId: Long)
}