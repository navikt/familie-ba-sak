package no.nav.familie.ba.sak.kjerne.eøs.utenlandsperiodebeløp

import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository

interface UtenlandskPeriodebeløpRepository : PeriodeOgBarnSkjemaRepository<UtenlandskPeriodebeløp> {
    // / TODO: Koble til databasen
    // @Query("")
    override fun findByBehandlingId(behandlingId: Long): List<UtenlandskPeriodebeløp>
}
