package no.nav.familie.ba.sak.behandling.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.*

interface FagsakRepository : JpaRepository<Fagsak?, Long?> {
    @Query(value = "SELECT f FROM Fagsak f WHERE f.id = :fagsakId")
    fun finnFagsak(fagsakId: Long?): Optional<Fagsak?>?
}