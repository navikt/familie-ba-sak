package no.nav.familie.ba.sak.tilbakekreving

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface TilbakekrevingRepository : JpaRepository<Tilbakekreving, Long> {

    @Query(value = "SELECT t FROM Tilbakekreving t JOIN t.behandling b WHERE b.id = :behandlingId")
    fun findByBehandlingId(behandlingId: Long): Tilbakekreving?
}