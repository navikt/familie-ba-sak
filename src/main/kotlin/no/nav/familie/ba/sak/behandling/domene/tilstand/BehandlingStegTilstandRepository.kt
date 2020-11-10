package no.nav.familie.ba.sak.behandling.domene.tilstand

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface BehandlingStegTilstandRepository : JpaRepository<BehandlingStegTilstand, Long> {
    @Query(value = "SELECT b FROM BehandlingStegTilstand b WHERE b.behandling.id = :behandlingId")
    fun finnBehandlingStegTilstand(behandlingId: Long): List<BehandlingStegTilstand>

    @Query(value = "SELECT b FROM BehandlingStegTilstand b WHERE b.behandling.id = :behandlingId and b.behandlingStegStatus = 'IKKE_UTFØRT'")
    fun finnSisteBehandlingStegTilstand(behandlingId: Long): BehandlingStegTilstand
}
