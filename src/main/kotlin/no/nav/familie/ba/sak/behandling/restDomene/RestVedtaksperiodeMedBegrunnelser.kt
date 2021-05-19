package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseSpesifikasjon
import java.time.LocalDate

data class RestVedtaksperiodeMedBegrunnelser(
        val id: Long,
        val fom: LocalDate?,
        val tom: LocalDate?,
        val type: Vedtaksperiodetype,
        val begrunnelser: List<RestVedtaksbegrunnelse>,
        val fritekster: List<String> = emptyList(),
)

data class RestVedtaksbegrunnelse(
        val vedtakBegrunnelseSpesifikasjon: VedtakBegrunnelseSpesifikasjon,
        val identer: List<String> = emptyList(),
)

data class RestPutVedtaksperiodeMedBegrunnelse(
        val begrunnelser: List<RestVedtaksbegrunnelse>,
        val fritekster: List<String> = emptyList(),
)