package no.nav.familie.ba.sak.behandling.vedtak.domene

import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface VedtaksperiodeRepository : JpaRepository<VedtaksperiodeMedBegrunnelser, Long> {

    @Modifying
    @Query("DELETE FROM Vedtaksperiode v WHERE v.vedtak = :vedtak")
    fun slettVedtaksperioderFor(vedtak: Vedtak)


    @Query(value = "SELECT v FROM Vedtaksperiode v WHERE v.id = :vedtaksperiodeId")
    fun finnVedtaksperiode(vedtaksperiodeId: Long): VedtaksperiodeMedBegrunnelser
    
    @Query("SELECT vp FROM Vedtaksperiode vp JOIN vp.vedtak v WHERE v.id = :vedtakId")
    fun finnVedtaksperioderFor(vedtakId: Long): List<VedtaksperiodeMedBegrunnelser>
}