package no.nav.familie.ba.sak.kjerne.vedtak.sammensattKontrollsak

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface SammensattKontrollsakRepository : JpaRepository<SammensattKontrollsak, Long> {
    @Query(value = "SELECT t FROM SammensattKontrollsak t WHERE t.behandlingId = :behandlingId")
    fun finnSammensattKontrollsakForBehandling(behandlingId: Long): SammensattKontrollsak?

    @Query(value = "SELECT t FROM SammensattKontrollsak t WHERE t.id = :id")
    fun hentSammensattKontrollsak(id: Long): SammensattKontrollsak
}
