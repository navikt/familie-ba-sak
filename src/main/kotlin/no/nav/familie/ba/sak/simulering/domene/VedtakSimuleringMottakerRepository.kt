package no.nav.familie.ba.sak.simulering.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional

interface VedtakSimuleringMottakerRepository : JpaRepository<VedtakSimuleringMottaker, Long> {

    @Query(value = "SELECT vsm FROM VedtakSimuleringMottaker vsm JOIN vsm.vedtak v WHERE v.id = :vedtakId")
    fun findByVedtakId(vedtakId: Long): List<VedtakSimuleringMottaker>

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM VedtakSimuleringMottaker vsm where vsm.vedtak.id = :vedtakId")
    fun deleteByVedtakId(vedtakId: Long)
}