package no.nav.familie.ba.sak.kjerne.vedtak.feilutbetaltValuta

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface FeilutbetaltValutaRepository : JpaRepository<FeilutbetaltValuta, Long> {
    @Query(value = "SELECT t FROM FeilutbetaltValuta t WHERE t.behandlingId = :behandlingId ORDER BY t.fom ASC")
    fun finnFeilutbetaltValutaForBehandling(behandlingId: Long): List<FeilutbetaltValuta>
}
