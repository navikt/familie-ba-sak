package no.nav.familie.ba.sak.behandling.vedtak

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

@Deprecated("Er erstattet av AndelTilkjentYtelseRepository")
interface VedtakPersonRepository : JpaRepository<VedtakPersonYtelsesperiode, Long> {
    @Query(value = "SELECT vb FROM VedtakPerson vb WHERE vb.vedtakId = :vedtakId")
    fun finnPersonBeregningForVedtak(vedtakId: Long): List<VedtakPersonYtelsesperiode>

    @Modifying
    @Query(value = "DELETE FROM VedtakPerson vb WHERE vb.vedtakId = :vedtakId")
    fun slettAllePersonBeregningerForVedtak(vedtakId: Long)
}

interface AndelTilkjentYtelseRepository : JpaRepository<AndelTilkjentYtelse, Long> {
    @Query(value = "SELECT aty FROM AndelTilkjentYtelse aty WHERE aty.behandlingId = :behandlingId")
    fun finnAndelTilkjentYtelseForBeregning(behandlingId: Long): List<AndelTilkjentYtelse>

    @Modifying
    @Query(value = "DELETE FROM AndelTilkjentYtelse aty WHERE aty.behandlingId = :behandlingId")
    fun slettAlleAndelTilkjentYtelseForBehandling(behandlingId: Long)
}