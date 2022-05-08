package no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene

import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
import org.springframework.data.jpa.repository.Query

interface KompetanseRepository : PeriodeOgBarnSkjemaRepository<Kompetanse> {

    @Query("SELECT k FROM Kompetanse k WHERE k.behandlingId = :behandlingId")
    override fun findByBehandlingId(behandlingId: Long): List<Kompetanse>
}
