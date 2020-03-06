package no.nav.familie.ba.sak.beregning.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface SatsRepository: JpaRepository<Sats, Long> {

    @Query("SELECT s FROM Sats s WHERE s.type = :type")
    fun finnAlleSatserFor(type: SatsType): List<Sats>
}