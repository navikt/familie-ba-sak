package no.nav.familie.ba.sak.kjerne.behandling.søknadreferanse

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SøknadReferanseRepository : JpaRepository<SøknadReferanse, Long> {
    fun findByBehandlingId(behandlingId: Long): SøknadReferanse?
}
