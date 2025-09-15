package no.nav.familie.ba.sak.kjerne.institusjon

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface InstitusjonsinfoRepository : JpaRepository<Institusjonsinfo, Long> {
    fun findByBehandlingId(behandlingId: Long): Institusjonsinfo?
}
