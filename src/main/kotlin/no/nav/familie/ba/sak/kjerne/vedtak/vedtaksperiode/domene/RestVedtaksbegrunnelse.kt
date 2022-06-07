package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene

import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType

data class RestVedtaksbegrunnelse(
    val standardbegrunnelse: IVedtakBegrunnelse,
    val vedtakBegrunnelseSpesifikasjon: IVedtakBegrunnelse,
    val vedtakBegrunnelseType: VedtakBegrunnelseType,
)
