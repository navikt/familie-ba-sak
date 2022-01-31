package no.nav.familie.ba.sak.kjerne.behandling.settpåvent

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Service

@Service
interface SettPåVentRepository : JpaRepository<SettPåVent, Long> {
    fun findByBehandlingIdAndAktiv(behandlingId: Long, aktiv: Boolean): SettPåVent?
}
