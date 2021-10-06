package no.nav.familie.ba.sak.kjerne.grunnlag.småbarnstillegg

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface PeriodeOvergangsstønadGrunnlagRepository : JpaRepository<PeriodeOvergangsstønadGrunnlag, Long> {

    @Query("SELECT poi FROM PeriodeOvergangsstønadIntern poi WHERE poi.behandlingId = :behandlingId")
    fun findByBehandlingId(behandlingId: Long): List<PeriodeOvergangsstønadGrunnlag>

    @Query("DELETE FROM PeriodeOvergangsstønadIntern poi WHERE poi.behandlingId = :behandlingId")
    @Modifying
    fun deleteByBehandlingId(behandlingId: Long)
}
