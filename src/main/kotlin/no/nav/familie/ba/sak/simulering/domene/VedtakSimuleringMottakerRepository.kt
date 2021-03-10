package no.nav.familie.ba.sak.simulering.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface VedtakSimuleringMottakerRepository : JpaRepository<VedtakSimuleringMottaker, Long> {

    @Query(value = "SELECT vsm FROM VedtakSimuleringMottaker vsm JOIN vsm.vedtak v WHERE v.id = :vedtakId")
    fun findByVedtakId(vedtakId: Long): List<VedtakSimuleringMottaker>
}