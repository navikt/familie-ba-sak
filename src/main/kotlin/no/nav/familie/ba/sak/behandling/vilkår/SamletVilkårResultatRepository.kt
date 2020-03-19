package no.nav.familie.ba.sak.behandling.vilkår

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface SamletVilkårResultatRepository : JpaRepository<SamletVilkårResultat, Long> {
    @Query(value = "SELECT svr FROM SamletVilkårResultat svr WHERE svr.behandlingId = :behandlingsId and svr.aktiv = true")
    fun finnSamletVilkårResultatPåBehandlingOgAktiv(behandlingsId: Long): SamletVilkårResultat?
}
