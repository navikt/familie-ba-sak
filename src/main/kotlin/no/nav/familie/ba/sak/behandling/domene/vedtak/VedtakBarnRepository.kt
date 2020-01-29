package no.nav.familie.ba.sak.behandling.domene.vedtak

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface VedtakBarnRepository : JpaRepository<VedtakBarn?, Long?> {
    @Query(value = "SELECT vb FROM VedtakBarn vb JOIN vb.vedtak v WHERE v.id = :vedtakId")
    fun finnBarnBeregningForVedtak(vedtakId: Long?): List<VedtakBarn>
}