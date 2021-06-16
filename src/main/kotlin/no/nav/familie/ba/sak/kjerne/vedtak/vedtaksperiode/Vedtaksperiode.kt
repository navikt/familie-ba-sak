package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import java.time.LocalDate

interface Vedtaksperiode {

    val periodeFom: LocalDate?
    val periodeTom: LocalDate?
    val vedtaksperiodetype: Vedtaksperiodetype
}

enum class Vedtaksperiodetype {
    UTBETALING,
    OPPHØR,
    AVSLAG,
    FORTSATT_INNVILGET
}

fun Vedtaksperiodetype.toVedtakFritekstBegrunnelseSpesifikasjon(): VedtakBegrunnelseSpesifikasjon = when (this) {
    Vedtaksperiodetype.OPPHØR -> VedtakBegrunnelseSpesifikasjon.OPPHØR_FRITEKST
    Vedtaksperiodetype.AVSLAG -> VedtakBegrunnelseSpesifikasjon.AVSLAG_FRITEKST
    Vedtaksperiodetype.UTBETALING -> VedtakBegrunnelseSpesifikasjon.REDUKSJON_FRITEKST
    Vedtaksperiodetype.FORTSATT_INNVILGET -> VedtakBegrunnelseSpesifikasjon.FORTSATT_INNVILGET_FRITEKST
}

