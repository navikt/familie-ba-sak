package no.nav.familie.ba.sak.kjerne.behandling.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface BehandlingSøknadsinfoRepository : JpaRepository<BehandlingSøknadsinfo, Long> {

    @Query("SELECT bs FROM BehandlingSøknadsinfo bs where bs.behandling.id=:behandlingId ")
    fun findByBehandlingId(behandlingId: Long): BehandlingSøknadsinfo?
}
