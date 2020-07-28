package no.nav.familie.ba.sak.beregning.domene

import no.nav.familie.ba.sak.behandling.domene.Behandling
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface TilkjentYtelseRepository: JpaRepository<TilkjentYtelse, Long> {
    @Modifying
    @Query("DELETE FROM TilkjentYtelse ty WHERE ty.behandling = :behandling")
    fun slettTilkjentYtelseFor(behandling: Behandling)

    @Query("SELECT ty FROM TilkjentYtelse ty JOIN ty.behandling b WHERE b.id = :behandlingId")
    fun findByBehandling(behandlingId: Long): TilkjentYtelse

    @Query("SELECT ty FROM TilkjentYtelse ty JOIN ty.behandling b WHERE b.id = :behandlingId")
    fun findByBehandlingOptional(behandlingId: Long): TilkjentYtelse?

    @Query("SELECT ty FROM TilkjentYtelse ty JOIN ty.behandling b WHERE b.id = :behandlingId AND ty.utbetalingsoppdrag is not null")
    fun findByBehandlingAndHasUtbetalingsoppdrag(behandlingId: Long): TilkjentYtelse?
}