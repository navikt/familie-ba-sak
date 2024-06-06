package no.nav.familie.ba.sak.kjerne.eøs.kompetanse

import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import org.springframework.data.jpa.repository.Query

interface KompetanseRepository : PeriodeOgBarnSkjemaRepository<Kompetanse> {
    @Query("SELECT k FROM Kompetanse k WHERE k.behandlingId = :behandlingId")
    override fun finnFraBehandlingId(behandlingId: Long): Collection<Kompetanse>

    @Query("SELECT k FROM Kompetanse k WHERE k.behandlingId in :behandlinger and k.resultat = 'NORGE_ER_SEKUNDÆRLAND'")
    fun hentSekundærlandsKompetanserForBehandlinger(behandlinger: Set<Long>): Collection<Kompetanse>
}
