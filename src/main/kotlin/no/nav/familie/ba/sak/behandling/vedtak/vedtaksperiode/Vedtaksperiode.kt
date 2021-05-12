package no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.common.Feil
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
    else -> throw Feil("Vedtaksperiodetype $this støtter ikke fritekst")
}

