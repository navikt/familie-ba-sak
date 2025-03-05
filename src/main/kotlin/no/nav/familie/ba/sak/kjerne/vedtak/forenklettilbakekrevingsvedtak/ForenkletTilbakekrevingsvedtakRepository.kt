package no.nav.familie.ba.sak.kjerne.vedtak.forenklettilbakekrevingsvedtak

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ForenkletTilbakekrevingsvedtakRepository : JpaRepository<ForenkletTilbakekrevingsvedtak, Long> {
    @Query(value = "SELECT ftv FROM ForenkletTilbakekrevingsvedtak ftv WHERE ftv.behandlingId = :behandlingId")
    fun finnForenkletTilbakekrevingsvedtakForBehandling(behandlingId: Long): ForenkletTilbakekrevingsvedtak?
}
