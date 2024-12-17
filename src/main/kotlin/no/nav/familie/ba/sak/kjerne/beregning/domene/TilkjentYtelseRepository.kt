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

    @Query(
        """
            SELECT EXISTS(
                SELECT 1 FROM Behandling b
                JOIN TilkjentYtelse ty ON ty.behandling.id = b.id
                WHERE ty.utbetalingsoppdrag IS NOT NULL AND ty.utbetalingsoppdrag like '%"klassifisering":"BAUTV-OP"%' and b.fagsak.id = :fagsakId
            )
        """,
    )
    fun harFagsakTattIBrukNyKlassekodeForUtvidetBarnetrygd(fagsakId: Long): Boolean

    @Query(
        """
        SELECT ty FROM Behandling b
        JOIN TilkjentYtelse ty ON ty.behandling.id =  b.id
        WHERE ty.utbetalingsoppdrag IS NOT NULL AND ty.utbetalingsoppdrag like '%"klassifisering":"BAUTV-OP"%' AND b.fagsak.id = :fagsakId 
    """,
    )
    fun finnUtbetalingsoppdragMedUtvidetBarnetrygd(fagsakId: Long): List<TilkjentYtelse>
}
