package no.nav.familie.ba.sak.behandling.vedtak.domene

import no.nav.familie.ba.sak.behandling.domene.Behandling
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface VedtaksperiodeRepository : JpaRepository<VedtaksperiodeMedBegrunnelser, Long> {

    @Modifying
    @Query("DELETE FROM Vedtaksperiode v WHERE v.behandling = :behandling")
    fun slettVedtaksperioderFor(behandling: Behandling)

    @Query("SELECT v FROM Vedtaksperiode v WHERE v.behandling = :behandling")
    fun finnVedtaksperioderFor(behandling: Behandling): List<VedtaksperiodeMedBegrunnelser>

    @Query(value = "SELECT v FROM Vedtaksperiode v WHERE v.id = :vedtaksperiodeId")
    fun finnVedtaksperiode(vedtaksperiodeId: Long): VedtaksperiodeMedBegrunnelser
}