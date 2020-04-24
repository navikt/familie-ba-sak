package no.nav.familie.ba.sak.beregning.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

interface SatsRepository  {
    fun finnAlleSatserFor(type: SatsType): List<Sats>
}