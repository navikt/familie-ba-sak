package no.nav.familie.ba.sak.behandling.vilkår

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PeriodeResultatRepository : JpaRepository<PeriodeResultat, Long> {
    @Query(value = "SELECT svr FROM PeriodeResultat svr WHERE svr.behandlingId = :behandlingsId and svr.aktiv = true")
    fun finnPeriodeResultatPåBehandlingOgAktiv(behandlingsId: Long): PeriodeResultat?
}
