package no.nav.familie.ba.sak.kjerne.vedtak.forenklettilbakekrevingvedtak

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ForenkletTilbakekrevingVedtakRepository : JpaRepository<ForenkletTilbakekrevingVedtak, Long> {
    @Query(value = "SELECT ftv FROM ForenkletTilbakekrevingVedtak ftv WHERE ftv.behandlingId = :behandlingId")
    fun finnForenkletTilbakekrevingVedtakForBehandling(behandlingId: Long): ForenkletTilbakekrevingVedtak?

    @Query(value = "SELECT ftv FROM ForenkletTilbakekrevingVedtak ftv WHERE ftv.id = :id")
    fun hentForenkletTilbakekrevingVedtak(id: Long): ForenkletTilbakekrevingVedtak
}
