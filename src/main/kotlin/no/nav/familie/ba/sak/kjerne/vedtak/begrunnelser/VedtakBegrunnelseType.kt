package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser

enum class VedtakBegrunnelseType(
    val sorteringsrekkefølge: Int,
) {
    INNVILGET(2),
    EØS_INNVILGET(2),
    INSTITUSJON_INNVILGET(2),
    REDUKSJON(1),
    INSTITUSJON_REDUKSJON(1),
    EØS_REDUKSJON(1),
    AVSLAG(3),
    EØS_AVSLAG(3),
    INSTITUSJON_AVSLAG(3),
    OPPHØR(4),
    EØS_OPPHØR(4),
    INSTITUSJON_OPPHØR(4),
    FORTSATT_INNVILGET(5),
    EØS_FORTSATT_INNVILGET(5),
    INSTITUSJON_FORTSATT_INNVILGET(5),
    ETTER_ENDRET_UTBETALING(6),
    ENDRET_UTBETALING(7),
    EØS_ENDRET_UTBETALING(7),
    ;

    fun erInnvilget(): Boolean = this == INNVILGET || this == INSTITUSJON_INNVILGET

    fun erReduksjon(): Boolean = this == REDUKSJON || this == INSTITUSJON_REDUKSJON

    fun erAvslag(): Boolean = this == AVSLAG || this == INSTITUSJON_AVSLAG || this == EØS_AVSLAG

    fun erEndretUtbetaling() = this == ENDRET_UTBETALING || this == EØS_ENDRET_UTBETALING
}
