package no.nav.familie.ba.sak.kjerne.registrertsøknadstidspunkt

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional

interface RegistrertSøknadstidspunktPåPersonRepository : JpaRepository<RegistrertSøknadstidspunktPåPerson, Long> {
    @Query("SELECT s FROM RegistrertSøknadstidspunktPåPerson s WHERE s.behandlingId = :behandlingId")
    fun findByBehandlingId(behandlingId: Long): List<RegistrertSøknadstidspunktPåPerson>

    @Transactional
    @Modifying
    @Query("DELETE FROM RegistrertSøknadstidspunktPåPerson s WHERE s.behandlingId = :behandlingId")
    fun deleteByBehandlingId(behandlingId: Long)
}
