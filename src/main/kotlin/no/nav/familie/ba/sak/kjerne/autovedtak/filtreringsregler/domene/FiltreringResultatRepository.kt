package no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface FiltreringResultatRepository : JpaRepository<FiltreringResultat, Long> {
    @Query(value = "SELECT f FROM FiltreringResultat f WHERE f.behandlingId = :behandlingId")
    fun finnFiltreringResultater(behandlingId: Long): List<FiltreringResultat>
}
