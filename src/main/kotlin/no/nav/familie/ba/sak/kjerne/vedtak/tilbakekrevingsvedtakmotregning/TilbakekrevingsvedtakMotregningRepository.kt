package no.nav.familie.ba.sak.kjerne.vedtak.tilbakekrevingsvedtakmotregning

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional

interface TilbakekrevingsvedtakMotregningRepository : JpaRepository<TilbakekrevingsvedtakMotregning, Long> {
    @Transactional(readOnly = true)
    @Query("SELECT ftv FROM TilbakekrevingsvedtakMotregning ftv JOIN ftv.behandling b WHERE b.id = :behandlingId")
    fun finnTilbakekrevingsvedtakMotregningForBehandling(behandlingId: Long): TilbakekrevingsvedtakMotregning?
}
