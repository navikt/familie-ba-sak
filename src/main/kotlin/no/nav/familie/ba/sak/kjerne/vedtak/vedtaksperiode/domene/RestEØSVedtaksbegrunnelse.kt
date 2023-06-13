package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene

import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType

data class RestEØSVedtaksbegrunnelse(
    val eøsStandardbegrunnelse: EØSStandardbegrunnelse,
    val vedtakBegrunnelseType: VedtakBegrunnelseType,
)
