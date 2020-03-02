package no.nav.familie.ba.sak.behandling.vedtak

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface VedtakRepository : JpaRepository<Vedtak, Long> {
    @Query(value = "SELECT v FROM Vedtak v JOIN v.behandling b WHERE b.id = :behandlingId")
    fun finnVedtakForBehandling(behandlingId: Long): List<Vedtak>

    @Query("SELECT v FROM Vedtak v JOIN v.behandling b WHERE b.id = :behandlingId AND v.aktiv = true")
    fun findByBehandlingAndAktiv(behandlingId: Long): Vedtak?
}