package no.nav.familie.ba.sak.kjerne.eøs.valutakurs

import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository

interface ValutakursRepository : PeriodeOgBarnSkjemaRepository<Valutakurs> {

    // / TODO: Koble til databasen
    // @Query("")
    override fun findByBehandlingId(behandlingId: Long): List<Valutakurs>
}
