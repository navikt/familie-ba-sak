package no.nav.familie.ba.sak.kjerne.beregning.domene

import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface TilkjentYtelseRepository : JpaRepository<TilkjentYtelse, Long> {
    @Modifying
    @Query("DELETE FROM TilkjentYtelse ty WHERE ty.behandling = :behandling")
    fun slettTilkjentYtelseFor(behandling: Behandling)

    @Query("SELECT ty FROM TilkjentYtelse ty JOIN ty.behandling b WHERE b.id = :behandlingId")
    fun findByBehandling(behandlingId: Long): TilkjentYtelse

    @Query("SELECT ty FROM TilkjentYtelse ty JOIN ty.behandling b WHERE b.id = :behandlingId")
    fun findByBehandlingOptional(behandlingId: Long): TilkjentYtelse?

    @Query("SELECT ty FROM TilkjentYtelse ty JOIN ty.behandling b WHERE b.id = :behandlingId AND ty.utbetalingsoppdrag is not null")
    fun findByBehandlingAndHasUtbetalingsoppdrag(behandlingId: Long): TilkjentYtelse?

    @Query("select ty from TilkjentYtelse ty JOIN Behandling b on b.id = ty.behandling.id where DATE(ty.endretDato) > '2023-06-7 00:00:00.000000' and Date(ty.endretDato) < '2023-08-15 00:00:00.000000' and ty.utbetalingsoppdrag is not null and ty.opphørFom is not null")
    fun findTilkjentYtelseMedFeilUtbetalingsoppdrag(): List<TilkjentYtelse>
}
