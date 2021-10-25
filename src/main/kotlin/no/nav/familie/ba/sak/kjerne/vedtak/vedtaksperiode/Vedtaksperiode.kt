package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import java.time.LocalDate

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "vedtaksperiodetype")
@JsonSubTypes(
    JsonSubTypes.Type(value = Utbetalingsperiode::class, name = "UTBETALING"),
    JsonSubTypes.Type(value = Avslagsperiode::class, name = "AVSLAG"),
    JsonSubTypes.Type(value = Opphørsperiode::class, name = "OPPHØR")
)
interface Vedtaksperiode {

    val periodeFom: LocalDate?
    val periodeTom: LocalDate?
    val vedtaksperiodetype: Vedtaksperiodetype
}

enum class Vedtaksperiodetype(val tillatteBegrunnelsestyper: List<VedtakBegrunnelseType>) {
    UTBETALING(
        listOf(
            VedtakBegrunnelseType.INNVILGET,
            VedtakBegrunnelseType.REDUKSJON,
            VedtakBegrunnelseType.FORTSATT_INNVILGET
        )
    ),
    OPPHØR(listOf(VedtakBegrunnelseType.OPPHØR)),
    AVSLAG(listOf(VedtakBegrunnelseType.AVSLAG)),
    FORTSATT_INNVILGET(listOf(VedtakBegrunnelseType.FORTSATT_INNVILGET)),
    ENDRET_UTBETALING(listOf(VedtakBegrunnelseType.INNVILGET, VedtakBegrunnelseType.REDUKSJON))
}

fun Vedtaksperiode.tilVedtaksperiodeMedBegrunnelse(
    vedtak: Vedtak,
): VedtaksperiodeMedBegrunnelser {

    return VedtaksperiodeMedBegrunnelser(
        fom = this.periodeFom,
        tom = this.periodeTom,
        vedtak = vedtak,
        type = this.vedtaksperiodetype
    )
}
