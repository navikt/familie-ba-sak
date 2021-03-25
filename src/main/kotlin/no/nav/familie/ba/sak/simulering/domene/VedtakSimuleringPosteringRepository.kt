package no.nav.familie.ba.sak.simulering.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional

interface VedtakSimuleringPosteringRepository : JpaRepository<VedtakSimuleringPostering, Long> {

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM VedtakSimuleringPostering vsp where vsp.vedtakSimuleringMottaker.id = :vedtakSimuleringMottakerId")
    fun deleteByVedtakSimuleringMottakerId(vedtakSimuleringMottakerId: Long)

}