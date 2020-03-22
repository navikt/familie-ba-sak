package no.nav.familie.ba.sak.behandling.vedtak

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface VedtakPersonRepository : JpaRepository<VedtakPerson, Long> {
    @Query(value = "SELECT vb FROM VedtakPerson vb WHERE vb.vedtakId = :vedtakId")
    fun finnPersonBeregningForVedtak(vedtakId: Long): List<VedtakPerson>
}