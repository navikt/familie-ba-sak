package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene

import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType

data class VedtaksbegrunnelseDto(
    val standardbegrunnelse: String,
    val vedtakBegrunnelseSpesifikasjon: String,
    val vedtakBegrunnelseType: VedtakBegrunnelseType,
    val støtterFritekst: Boolean,
)
