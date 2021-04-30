package no.nav.familie.ba.sak.simulering.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional

interface BehandlingSimuleringPosteringRepository : JpaRepository<BrSimuleringPostering, Long> {

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM BrSimuleringPostering sp where sp.brSimuleringMottaker.id = :vedtakSimuleringMottakerId")
    fun deleteByVedtakSimuleringMottakerId(vedtakSimuleringMottakerId: Long)

}