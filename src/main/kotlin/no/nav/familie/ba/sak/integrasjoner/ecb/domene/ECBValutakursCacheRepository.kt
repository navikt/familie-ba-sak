package no.nav.familie.ba.sak.integrasjoner.ecb.domene

import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface ECBValutakursCacheRepository : JpaRepository<ECBValutakursCache, Long> {
    fun findByValutakodeAndValutakursdato(
        valutakode: String,
        valutakursdato: LocalDate,
    ): List<ECBValutakursCache>?
}
