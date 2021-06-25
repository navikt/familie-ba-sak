package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import java.time.LocalDate

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "vedtaksperiodetype")
@JsonSubTypes(JsonSubTypes.Type(value = Utbetalingsperiode::class, name = "UTBETALING"),
              JsonSubTypes.Type(value = Avslagsperiode::class, name = "AVSLAG"),
              JsonSubTypes.Type(value = Opphørsperiode::class, name = "OPPHØR"))
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

