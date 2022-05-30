package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene

import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType

data class RestVedtaksbegrunnelse(
    val standardbegrunnelse: Standardbegrunnelse,
    val vedtakBegrunnelseType: VedtakBegrunnelseType,
)
