package no.nav.familie.ba.sak.kjerne.vedtak.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface VedtaksbegrunnelseRepository : JpaRepository<Vedtaksbegrunnelse, Long> {
    @Query(
        """
        SELECT begrunnelse
        FROM Vedtaksbegrunnelse begrunnelse
        JOIN
            begrunnelse.vedtaksperiodeMedBegrunnelser periode,
            periode.vedtak vedtak,
            vedtak.behandling behandling
        WHERE
            behandling.id = :behandingsId
        """
    )
    fun hentAlleVedtakbegrunnelserPåBehandling(behandlingsId: Long): List<Vedtaksbegrunnelse>
}
