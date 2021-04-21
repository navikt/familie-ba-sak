package no.nav.familie.ba.sak.simulering.tilbakekreving

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface TilbakekrevingRepository : JpaRepository<Tilbakekreving, Long> {

    @Query(value = "SELECT t FROM Tilbakekreving t JOIN t.vedtak v WHERE v.id = :vedtakId")
    fun findByVedtakId(vedtakId: Long): Tilbakekreving?
}