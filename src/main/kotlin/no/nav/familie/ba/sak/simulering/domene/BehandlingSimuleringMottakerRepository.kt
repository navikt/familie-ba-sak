package no.nav.familie.ba.sak.simulering.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional

interface BehandlingSimuleringMottakerRepository : JpaRepository<BrSimuleringMottaker, Long> {

    @Query(value = "SELECT sm FROM BrSimuleringMottaker sm JOIN sm.behandling b WHERE b.id = :behandlingId")
    fun findByBehandlingId(behandlingId: Long): List<BrSimuleringMottaker>

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM BrSimuleringMottaker sm where sm.behandling.id = :vedtakbehandlingIdId")
    fun deleteByBehandlingId(behandlingId: Long)
}