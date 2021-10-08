package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
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

enum class Vedtaksperiodetype {
    UTBETALING,
    OPPHØR,
    AVSLAG,
    FORTSATT_INNVILGET,
    ENDRET_UTBETALING
}

fun Vedtaksperiode.tilVedtaksperiodeMedBegrunnelse(
    vedtak: Vedtak
): VedtaksperiodeMedBegrunnelser {
    return VedtaksperiodeMedBegrunnelser(
        fom = this.periodeFom,
        tom = this.periodeTom,
        vedtak = vedtak,
        type = this.vedtaksperiodetype,
        begrunnelser = mutableSetOf()
    ).also { vedtaksperiodeMedBegrunnelser ->
        if (this is EndretUtbetalingsperiode)
            vedtaksperiodeMedBegrunnelser.begrunnelser.addAll(
                (this.endretUtbetalingAndel.vedtakBegrunnelseSpesifikasjoner).map { vedtakBegrunnelseSpesifikasjon ->
                    Vedtaksbegrunnelse(
                        vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
                        vedtakBegrunnelseSpesifikasjon = vedtakBegrunnelseSpesifikasjon,
                        personIdenter = listOf(this.endretUtbetalingAndel.person!!.personIdent.ident)
                    )
                })
    }
}
