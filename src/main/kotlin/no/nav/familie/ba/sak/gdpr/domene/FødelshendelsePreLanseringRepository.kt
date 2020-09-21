package no.nav.familie.ba.sak.gdpr.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface FødelshendelsePreLanseringRepository: JpaRepository<FødselshendelsePreLansering, Long> {
    @Query(value = "SELECT fpl FROM FødselshendelsePreLansering fpl WHERE fpl.behandlingId = :behandlingId")
    fun finnFødselshendelsePreLansering(behandlingId: Long): FødselshendelsePreLansering
}