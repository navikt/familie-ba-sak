package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SatskjøringRepository : JpaRepository<Satskjøring, Long> {
    fun countByFerdigTidspunktIsNotNull(): Long
    fun findByFagsakId(fagsakId: Long): Satskjøring?
}
