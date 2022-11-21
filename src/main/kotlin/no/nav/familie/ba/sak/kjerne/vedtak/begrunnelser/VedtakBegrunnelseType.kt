package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.forrigeMåned
import no.nav.familie.ba.sak.common.tilMånedÅr

enum class VedtakBegrunnelseType(val sorteringsrekkefølge: Int) {
    INNVILGET(2),
    EØS_INNVILGET(2),
    INSTITUSJON_INNVILGET(2),
    REDUKSJON(1),
    INSTITUSJON_REDUKSJON(1),
    AVSLAG(3),
    INSTITUSJON_AVSLAG(3),
    OPPHØR(4),
    EØS_OPPHØR(4),
    INSTITUSJON_OPPHØR(4),
    FORTSATT_INNVILGET(5),
    INSTITUSJON_FORTSATT_INNVILGET(5),
    ENDRET_UTBETALING(7),
    ETTER_ENDRET_UTBETALING(6);

    fun erInnvilget(): Boolean {
        return this == INNVILGET || this == INSTITUSJON_INNVILGET
    }

    fun erFortsattInnvilget(): Boolean {
        return this == FORTSATT_INNVILGET || this == INSTITUSJON_FORTSATT_INNVILGET
    }

    fun erReduksjon(): Boolean {
        return this == REDUKSJON || this == INSTITUSJON_REDUKSJON
    }

    fun erAvslag(): Boolean {
        return this == AVSLAG || this == INSTITUSJON_AVSLAG
    }

    fun erOpphør(): Boolean {
        return this == OPPHØR || this == INSTITUSJON_OPPHØR
    }
}

fun VedtakBegrunnelseType.hentMånedOgÅrForBegrunnelse(periode: Periode) = when (this) {
    VedtakBegrunnelseType.AVSLAG, VedtakBegrunnelseType.INSTITUSJON_AVSLAG ->
        if (periode.fom == TIDENES_MORGEN && periode.tom == TIDENES_ENDE) {
            ""
        } else if (periode.tom == TIDENES_ENDE) {
            periode.fom.tilMånedÅr()
        } else {
            "${periode.fom.tilMånedÅr()} til ${periode.tom.tilMånedÅr()}"
        }

    else ->
        if (periode.fom == TIDENES_MORGEN) {
            throw Feil("Prøver å finne fom-dato for begrunnelse, men fikk \"TIDENES_MORGEN\".")
        } else {
            periode.fom.forrigeMåned().tilMånedÅr()
        }
}
