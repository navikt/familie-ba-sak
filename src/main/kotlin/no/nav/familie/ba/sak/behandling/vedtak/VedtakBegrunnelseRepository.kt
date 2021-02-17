package no.nav.familie.ba.sak.behandling.vedtak

import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseSpesifikasjon
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface VedtakBegrunnelseRepository : JpaRepository<VedtakBegrunnelse, Long> {

    @Query(value = """select vb from VedtakBegrunnelse vb
                       inner join Vedtak v on vb.vedtak = v
                       inner join Behandling b on b = v.behandling and b.fagsak.id = :fagsakId
                       where vb.begrunnelse = :vedtakBegrunnelse and vb.fom = :fom""")
    fun finnForFagsakMedBegrunnelseGyldigFom(
            fagsakId: Long,
            vedtakBegrunnelse: VedtakBegrunnelseSpesifikasjon,
            fom: LocalDate): List<VedtakBegrunnelse>
}