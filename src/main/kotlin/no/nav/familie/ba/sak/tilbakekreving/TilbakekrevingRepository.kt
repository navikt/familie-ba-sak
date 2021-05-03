package no.nav.familie.ba.sak.tilbakekreving

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional

interface TilbakekrevingRepository : JpaRepository<Tilbakekreving, Long> {

    @Query(value = "SELECT t FROM Tilbakekreving t JOIN t.behandling b WHERE b.id = :behandlingId")
    fun findByBehandlingId(behandlingId: Long): Tilbakekreving?

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM Tilbakekreving t WHERE t.behandling.id = :behandlingId")
    fun deleteByBehandlingId(behandlingId: Long)
}