package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional

interface VilkårResultatRepository : JpaRepository<VilkårResultat, Long> {
    @Query(
        "SELECT id FROM vilkar_resultat WHERE er_opprinnelig_preutfylt IS TRUE AND er_opprinnelig_preutfylt_i_behandling IS NULL LIMIT :antall",
        nativeQuery = true,
    )
    fun finnPreutfylteVilkårResultaterUtenBehandlingId(antall: Int): List<Long>

    @Modifying
    @Transactional
    @Query(
        "UPDATE vilkar_resultat SET er_opprinnelig_preutfylt_i_behandling = sist_endret_i_behandling_id WHERE id = :id",
        nativeQuery = true,
    )
    fun oppdaterErOpprinneligPreutfyltIBehandling(id: Long)
}
