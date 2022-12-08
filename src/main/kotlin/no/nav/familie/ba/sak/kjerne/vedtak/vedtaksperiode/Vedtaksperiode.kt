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
            VedtakBegrunnelseType.FORTSATT_INNVILGET,
            VedtakBegrunnelseType.ETTER_ENDRET_UTBETALING,
            VedtakBegrunnelseType.ENDRET_UTBETALING,
            VedtakBegrunnelseType.INSTITUSJON_INNVILGET,
            VedtakBegrunnelseType.INSTITUSJON_REDUKSJON,
            VedtakBegrunnelseType.INSTITUSJON_FORTSATT_INNVILGET
        )
    ),

    /***
     * Brukes i de tilfellene det er en reduksjon på tvers av behandlinger som vi ikke kan begrunne på vanlig måte.
     *
     * For eksempel: I en behandling har vi to barn med utbetaling fra mai 2020 til januar 2021.
     * I neste behandling endres det ene barne til å ha utbetaling fra juni 2020 til januar 2021.
     * Da har det vært en reduksjon fra den første behandlingen til den neste i mai 2020,
     * og det blir en utbetaling med reduksjon fra sist iverksatte behandling.
     *
     * Om det ene barnet hadde mistet juli isteden for mai, altså at det fikk utbetalt 1. mai 2020 til 1. juni 2021 og
     * fra juli 2020 til januar 2021, ville juni 2020 vært en vanlig utbetalingsperiode fordi vi kan begrunne
     * reduksjonen uten å se på forrige behandling.
     ***/
    UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING(
        listOf(
            VedtakBegrunnelseType.REDUKSJON,
            VedtakBegrunnelseType.INNVILGET,
            VedtakBegrunnelseType.ETTER_ENDRET_UTBETALING,
            VedtakBegrunnelseType.ENDRET_UTBETALING,
            VedtakBegrunnelseType.INSTITUSJON_REDUKSJON,
            VedtakBegrunnelseType.INSTITUSJON_INNVILGET
        )
    ),
    OPPHØR(
        listOf(
            VedtakBegrunnelseType.OPPHØR,
            VedtakBegrunnelseType.ETTER_ENDRET_UTBETALING,
            VedtakBegrunnelseType.INSTITUSJON_OPPHØR
        )
    ),
    AVSLAG(
        listOf(
            VedtakBegrunnelseType.AVSLAG, 
            VedtakBegrunnelseType.EØS_AVSLAG,
            VedtakBegrunnelseType.INSTITUSJON_AVSLAG
        )
    ),
    FORTSATT_INNVILGET(
        listOf(
            VedtakBegrunnelseType.FORTSATT_INNVILGET,
            VedtakBegrunnelseType.INSTITUSJON_FORTSATT_INNVILGET
        )
    ),

    @Deprecated("Legacy")
    ENDRET_UTBETALING(emptyList())
}

fun Vedtaksperiode.tilVedtaksperiodeMedBegrunnelse(
    vedtak: Vedtak
): VedtaksperiodeMedBegrunnelser {
    return VedtaksperiodeMedBegrunnelser(
        fom = this.periodeFom,
        tom = this.periodeTom,
        vedtak = vedtak,
        type = this.vedtaksperiodetype
    )
}
