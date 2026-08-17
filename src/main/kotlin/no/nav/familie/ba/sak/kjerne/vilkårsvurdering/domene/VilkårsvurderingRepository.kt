package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface VilkårsvurderingRepository : JpaRepository<Vilkårsvurdering, Long> {
    @Query("SELECT v FROM Vilkårsvurdering v JOIN v.behandling b WHERE b.id = :behandlingId AND v.aktiv = true")
    fun findByBehandlingAndAktiv(behandlingId: Long): Vilkårsvurdering?

    @Query("SELECT v.id FROM Vilkårsvurdering v WHERE v.aktiv = false AND v.id > :etterId ORDER BY v.id")
    fun finnIderForInaktiveVilkårsvurderinger(
        etterId: Long,
        pageable: Pageable,
    ): List<Long>

    @Modifying
    @Query(
        """
        DELETE FROM AnnenVurdering av
        WHERE av.personResultat.id IN (SELECT pr.id FROM PersonResultat pr WHERE pr.vilkårsvurdering.id IN :vilkårsvurderingIder)
        """,
    )
    fun slettAndreVurderingerForVilkårsvurderinger(vilkårsvurderingIder: List<Long>)

    @Modifying
    @Query(
        """
        DELETE FROM VilkårResultat vr
        WHERE vr.personResultat.id IN (SELECT pr.id FROM PersonResultat pr WHERE pr.vilkårsvurdering.id IN :vilkårsvurderingIder)
        """,
    )
    fun slettVilkårResultaterForVilkårsvurderinger(vilkårsvurderingIder: List<Long>)

    @Modifying
    @Query("DELETE FROM PersonResultat pr WHERE pr.vilkårsvurdering.id IN :vilkårsvurderingIder")
    fun slettPersonResultaterForVilkårsvurderinger(vilkårsvurderingIder: List<Long>)

    @Modifying
    @Query("DELETE FROM Vilkårsvurdering v WHERE v.id IN :vilkårsvurderingIder")
    fun slettVilkårsvurderinger(vilkårsvurderingIder: List<Long>)
}
