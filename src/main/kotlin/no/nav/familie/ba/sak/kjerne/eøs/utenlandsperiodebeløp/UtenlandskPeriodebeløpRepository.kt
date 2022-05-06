package no.nav.familie.ba.sak.kjerne.eøs.utenlandsperiodebeløp

import org.springframework.data.jpa.repository.JpaRepository

interface UtenlandskPeriodebeløpRepository : JpaRepository<UtenlandskPeriodebeløp, Long> {

    // / TODO: Koble til databasen
    // @Query("")
    fun findByBehandlingId(behandlingId: Long): List<UtenlandskPeriodebeløp>
}
