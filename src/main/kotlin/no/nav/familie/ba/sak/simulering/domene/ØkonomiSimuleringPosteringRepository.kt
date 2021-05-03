package no.nav.familie.ba.sak.simulering.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional

interface ØkonomiSimuleringPosteringRepository : JpaRepository<ØkonomiSimuleringPostering, Long> {

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM OkonomiSimuleringPostering sp where sp.økonomiSimuleringMottaker.id = :økonomiSimuleringMottakerId")
    fun deleteByVedtakSimuleringMottakerId(økonomiSimuleringMottakerId: Long)

}