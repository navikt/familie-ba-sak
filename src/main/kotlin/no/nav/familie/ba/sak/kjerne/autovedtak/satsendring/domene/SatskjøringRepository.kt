package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.YearMonth

@Repository
interface SatskjøringRepository : JpaRepository<Satskjøring, Long> {
    fun countByFerdigTidspunktIsNotNull(): Long
    fun findByFagsakIdAndSatsTidspunkt(fagsakId: Long, satsTidspunkt: YearMonth): Satskjøring?
}

interface SatskjøringÅpenBehandling {
    val fagsakId: Long
    val behandlingId: Long
}
