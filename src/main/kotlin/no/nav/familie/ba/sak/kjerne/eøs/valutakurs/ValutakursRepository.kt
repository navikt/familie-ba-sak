package no.nav.familie.ba.sak.kjerne.eøs.valutakurs

import org.springframework.data.jpa.repository.JpaRepository

interface ValutakursRepository : JpaRepository<Valutakurs, Long> {

    // / TODO: Koble til databasen
    // @Query("")
    fun findByBehandlingId(behandlingId: Long): List<Valutakurs>
}
