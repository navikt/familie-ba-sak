package no.nav.familie.ba.sak.beregning.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface TilkjentYtelseRepository: JpaRepository<TilkjentYtelse, Long> {

    @Query("SELECT br FROM TilkjentYtelse br JOIN br.behandling b WHERE b.id = :behandlingId")
    fun findByBehandling(behandlingId: Long): TilkjentYtelse
}