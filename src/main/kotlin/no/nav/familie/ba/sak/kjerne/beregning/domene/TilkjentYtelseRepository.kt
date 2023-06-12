package no.nav.familie.ba.sak.kjerne.beregning.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface TilkjentYtelseRepository : JpaRepository<TilkjentYtelse, Long> {
    @Modifying
    @Query("DELETE FROM TilkjentYtelse ty WHERE ty.behandlingId = :behandlingId")
    fun slettTilkjentYtelseFor(behandlingId: Long)

    @Query("SELECT ty FROM TilkjentYtelse ty WHERE ty.behandlingId = :behandlingId")
    fun findByBehandling(behandlingId: Long): TilkjentYtelse

    @Query("SELECT ty FROM TilkjentYtelse ty WHERE ty.behandlingId = :behandlingId")
    fun findByBehandlingOptional(behandlingId: Long): TilkjentYtelse?

    @Query("SELECT ty FROM TilkjentYtelse ty WHERE ty.behandlingId = :behandlingId AND ty.utbetalingsoppdrag is not null")
    fun findByBehandlingAndHasUtbetalingsoppdrag(behandlingId: Long): TilkjentYtelse?
}
