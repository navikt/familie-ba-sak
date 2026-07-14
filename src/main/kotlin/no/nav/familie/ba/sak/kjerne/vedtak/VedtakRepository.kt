package no.nav.familie.ba.sak.kjerne.vedtak

import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface VedtakRepository : JpaRepository<Vedtak, Long> {
    @Query(value = "SELECT v FROM Vedtak v WHERE v.behandling.id = :behandlingId")
    fun finnVedtakForBehandling(behandlingId: Long): List<Vedtak>

    @Query(
        """
        SELECT v.id FROM Vedtak v
        WHERE v.stønadBrevPdF IS NOT NULL
        AND v.behandling.status = :status
        AND v.vedtaksdato < :vedtaksdatoFør
        """,
    )
    fun finnVedtakIderMedStønadBrevPdf(
        status: BehandlingStatus,
        vedtaksdatoFør: LocalDateTime,
        pageable: Pageable,
    ): List<Long>

    @Modifying(clearAutomatically = true)
    @Query(
        """
        UPDATE Vedtak v
        SET v.stønadBrevPdF = null
        WHERE v.id IN :vedtakIder
        """,
    )
    fun slettStønadBrevPdfForVedtak(vedtakIder: List<Long>): Int

    @Query("SELECT v FROM Vedtak v WHERE v.behandling.id = :behandlingId AND v.aktiv = true")
    fun findByBehandlingAndAktivOptional(behandlingId: Long): Vedtak?

    @Query("SELECT v FROM Vedtak v WHERE v.behandling.id = :behandlingId AND v.aktiv = true")
    fun findByBehandlingAndAktiv(behandlingId: Long): Vedtak

    @Query("SELECT v.vedtaksdato FROM Vedtak v WHERE v.behandling.id = :behandlingId AND v.aktiv = true")
    fun finnVedtaksdatoForBehandling(behandlingId: Long): LocalDateTime?
}
