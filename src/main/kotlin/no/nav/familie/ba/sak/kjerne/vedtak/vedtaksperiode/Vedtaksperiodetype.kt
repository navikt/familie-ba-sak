package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType

enum class Vedtaksperiodetype(
    val tillatteBegrunnelsestyper: Set<VedtakBegrunnelseType>,
) {
    UTBETALING(
        setOf(
            VedtakBegrunnelseType.INNVILGET,
            VedtakBegrunnelseType.REDUKSJON,
            VedtakBegrunnelseType.FORTSATT_INNVILGET,
            VedtakBegrunnelseType.ETTER_ENDRET_UTBETALING,
            VedtakBegrunnelseType.ENDRET_UTBETALING,
            VedtakBegrunnelseType.EØS_ENDRET_UTBETALING,
            VedtakBegrunnelseType.INSTITUSJON_INNVILGET,
            VedtakBegrunnelseType.INSTITUSJON_REDUKSJON,
            VedtakBegrunnelseType.INSTITUSJON_FORTSATT_INNVILGET,
            VedtakBegrunnelseType.EØS_INNVILGET,
            VedtakBegrunnelseType.EØS_REDUKSJON,
            VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET,
        ),
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
        setOf(
            VedtakBegrunnelseType.REDUKSJON,
            VedtakBegrunnelseType.INNVILGET,
            VedtakBegrunnelseType.ETTER_ENDRET_UTBETALING,
            VedtakBegrunnelseType.ENDRET_UTBETALING,
            VedtakBegrunnelseType.EØS_ENDRET_UTBETALING,
            VedtakBegrunnelseType.INSTITUSJON_REDUKSJON,
            VedtakBegrunnelseType.INSTITUSJON_INNVILGET,
            VedtakBegrunnelseType.EØS_INNVILGET,
            VedtakBegrunnelseType.EØS_REDUKSJON,
        ),
    ),
    OPPHØR(
        setOf(
            VedtakBegrunnelseType.OPPHØR,
            VedtakBegrunnelseType.ENDRET_UTBETALING,
            VedtakBegrunnelseType.EØS_ENDRET_UTBETALING,
            VedtakBegrunnelseType.ETTER_ENDRET_UTBETALING,
            VedtakBegrunnelseType.INSTITUSJON_OPPHØR,
            VedtakBegrunnelseType.EØS_OPPHØR,
            VedtakBegrunnelseType.AVSLAG,
        ),
    ),
    AVSLAG(
        setOf(
            VedtakBegrunnelseType.AVSLAG,
            VedtakBegrunnelseType.EØS_AVSLAG,
            VedtakBegrunnelseType.INSTITUSJON_AVSLAG,
        ),
    ),
    FORTSATT_INNVILGET(
        setOf(
            VedtakBegrunnelseType.FORTSATT_INNVILGET,
            VedtakBegrunnelseType.INSTITUSJON_FORTSATT_INNVILGET,
            VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET,
        ),
    ),

    @Deprecated("Legacy. Kan ikke fjernes uten at det ryddes opp i Vedtaksperioder-tabellen og man kan ikke endre i tabellen fordi man ikke vet om det er en økning eller reduksjon")
    ENDRET_UTBETALING(emptySet()),
    ;

    @Suppress("DEPRECATION")
    fun sorteringsRekkefølge(): Int =
        when (this) {
            UTBETALING -> 1
            UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING -> 2
            FORTSATT_INNVILGET -> 3
            OPPHØR -> 4
            AVSLAG -> 5
            ENDRET_UTBETALING -> 6
        }
}
