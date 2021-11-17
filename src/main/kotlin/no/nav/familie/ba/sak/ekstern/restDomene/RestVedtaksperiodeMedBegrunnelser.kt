package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon

data class RestPutVedtaksperiodeMedFritekster(
    val fritekster: List<String> = emptyList(),
)

data class RestPutVedtaksperiodeMedStandardbegrunnelser(
    val standardbegrunnelser: List<VedtakBegrunnelseSpesifikasjon>,
)
