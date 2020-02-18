package no.nav.familie.ba.sak.behandling.domene.vedtak

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface VedtakPersonRepository : JpaRepository<VedtakPerson?, Long?> {
    @Query(value = "SELECT vb FROM VedtakPerson vb JOIN vb.vedtak v WHERE v.id = :vedtakId")
    fun finnPersonBeregningForVedtak(vedtakId: Long?): List<VedtakPerson>
}