package no.nav.familie.ba.sak.kjerne.registrertsøknadstidspunkt

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional

interface RegistrertSøknadstidspunktRepository : JpaRepository<RegistrertSøknadstidspunkt, Long> {
    @Query("SELECT s FROM RegistrertSøknadstidspunkt s WHERE s.behandlingId = :behandlingId")
    fun findByBehandlingId(behandlingId: Long): List<RegistrertSøknadstidspunkt>

    @Transactional
    @Modifying
    @Query("DELETE FROM RegistrertSøknadstidspunkt s WHERE s.behandlingId = :behandlingId")
    fun deleteByBehandlingId(behandlingId: Long)
}
