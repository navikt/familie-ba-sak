package no.nav.familie.ba.sak.kjerne.vedtak.forenklettilbakekrevingsvedtak

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional

interface ForenkletTilbakekrevingsvedtakRepository : JpaRepository<ForenkletTilbakekrevingsvedtak, Long> {
    @Transactional(readOnly = true)
    @Query("SELECT ftv FROM ForenkletTilbakekrevingsvedtak ftv JOIN ftv.behandling b WHERE b.id = :behandlingId")
    fun finnForenkletTilbakekrevingsvedtakForBehandling(behandlingId: Long): ForenkletTilbakekrevingsvedtak?
}
