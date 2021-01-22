package no.nav.familie.ba.sak.behandling.vedtak

import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelse
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface UtbetalingBegrunnelseRepository : JpaRepository<UtbetalingBegrunnelse, Long> {

    @Query(value = """select ub from UtbetalingBegrunnelse ub
                       inner join Vedtak v on ub.vedtak = v
                       inner join Behandling b on b = v.behandling and b.fagsak.id = :fagsakId
                       where ub.vedtakBegrunnelse = :vedtakBegrunnelse and ub.fom = :fom""")
    fun finnForFagsakMedBegrunnelseGyldigFom(
            fagsakId: Long,
            vedtakBegrunnelse: VedtakBegrunnelse,
            fom: LocalDate): UtbetalingBegrunnelse?
}