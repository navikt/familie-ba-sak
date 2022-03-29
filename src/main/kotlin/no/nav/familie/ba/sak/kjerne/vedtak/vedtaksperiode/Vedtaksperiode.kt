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

enum class Vedtaksperiodetype(val tillatteBegrunnelsestyper: (erIngenOverlappVedtaksperiodeToggelPå: Boolean) -> List<VedtakBegrunnelseType>) {
    UTBETALING(
        { erIngenOverlappVedtaksperiodeToggelPå ->
            val utbetalingstyperUtenEndretUtbetaling = listOf(
                VedtakBegrunnelseType.INNVILGET,
                VedtakBegrunnelseType.REDUKSJON,
                VedtakBegrunnelseType.FORTSATT_INNVILGET,
                VedtakBegrunnelseType.ETTER_ENDRET_UTBETALING
            )
            if (!erIngenOverlappVedtaksperiodeToggelPå)
                utbetalingstyperUtenEndretUtbetaling
            else
                utbetalingstyperUtenEndretUtbetaling.plus(VedtakBegrunnelseType.ENDRET_UTBETALING)
        }
    ),

    // For å kunne begrunne reduksjon mellom behandlinger i de tilfellene der vi ikke kan bruke den forrige perioden
    // til å begrunne reduksjonen.
    UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING({
        listOf(
            VedtakBegrunnelseType.REDUKSJON,
            VedtakBegrunnelseType.INNVILGET,
            VedtakBegrunnelseType.ETTER_ENDRET_UTBETALING,
            VedtakBegrunnelseType.ENDRET_UTBETALING
        )
    }),
    OPPHØR({ listOf(VedtakBegrunnelseType.OPPHØR) }),
    AVSLAG({ listOf(VedtakBegrunnelseType.AVSLAG) }),
    FORTSATT_INNVILGET({ listOf(VedtakBegrunnelseType.FORTSATT_INNVILGET) }),

    @Deprecated("Skal ikke brukes lenger. Fjernes når INGEN_OVERLAPP_VEDTAKSPERIODER-triggeren fjernes.")
    ENDRET_UTBETALING({
        listOf(
            VedtakBegrunnelseType.ENDRET_UTBETALING
        )
    }
    )
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
