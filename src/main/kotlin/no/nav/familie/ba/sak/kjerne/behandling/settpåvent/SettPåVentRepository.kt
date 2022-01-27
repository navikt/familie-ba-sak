package no.nav.familie.ba.sak.kjerne.behandling.settpåvent

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Service

@Service
interface SettPåVentRepository : JpaRepository<SettPåVent, Long> {
    fun findByBehandlingId(behandlingId: Long): List<SettPåVent>

    fun findByBehandlingIdAndAktiv(behandlingId: Long, aktiv: Boolean): SettPåVent?
}
