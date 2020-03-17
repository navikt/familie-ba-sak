package no.nav.familie.ba.sak.beregning.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface BeregningResultatRepository: JpaRepository<BeregningResultat, Long> {

    @Query("SELECT br FROM BehandlingResultat br JOIN br.behanding b WHERE b.id = :behandlingId")
    fun findByBehandling(behandlingId: Long): BeregningResultat
}