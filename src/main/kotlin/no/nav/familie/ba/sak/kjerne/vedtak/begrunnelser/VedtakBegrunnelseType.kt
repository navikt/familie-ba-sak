package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.forrigeMåned
import no.nav.familie.ba.sak.common.tilMånedÅr

enum class VedtakBegrunnelseType(val sorteringsrekkefølge: Int) {
    REDUKSJON(1),
    INNVILGET(2),
    AVSLAG(3),
    OPPHØR(4),
    FORTSATT_INNVILGET(5),
    ETTER_ENDRET_UTBETALING(6),
    ENDRET_UTBETALING(7),
}

fun VedtakBegrunnelseType.hentMånedOgÅrForBegrunnelse(periode: Periode) = when (this) {
    VedtakBegrunnelseType.AVSLAG ->
        if (periode.fom == TIDENES_MORGEN && periode.tom == TIDENES_ENDE) ""
        else if (periode.tom == TIDENES_ENDE) periode.fom.tilMånedÅr()
        else "${periode.fom.tilMånedÅr()} til ${periode.tom.tilMånedÅr()}"
    else ->
        if (periode.fom == TIDENES_MORGEN)
            throw Feil("Prøver å finne fom-dato for begrunnelse, men fikk \"TIDENES_MORGEN\".")
        else periode.fom.forrigeMåned().tilMånedÅr()
}
