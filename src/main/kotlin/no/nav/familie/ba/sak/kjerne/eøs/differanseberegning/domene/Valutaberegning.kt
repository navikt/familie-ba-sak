package no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene

import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs
import java.math.BigDecimal

data class KronerPerValutaenhet(
    val kronerPerValutaenhet: BigDecimal,
    val valutakode: String,
)

data class Valutabeløp(
    val beløp: BigDecimal,
    val valutakode: String,
)

operator fun Valutabeløp?.times(kronerPerValutaenhet: KronerPerValutaenhet?): BigDecimal? {
    if (this == null || kronerPerValutaenhet == null) {
        return null
    }

    if (this.valutakode != kronerPerValutaenhet.valutakode) {
        return null
    }

    return this.beløp * kronerPerValutaenhet.kronerPerValutaenhet
}

fun UtenlandskPeriodebeløp?.tilMånedligValutabeløp(): Valutabeløp? {
    val kalkulertMånedligBeløp = this?.kalkulertMånedligBeløp
    val valutakode = this?.valutakode
    if (kalkulertMånedligBeløp == null || valutakode == null) {
        return null
    }

    return Valutabeløp(kalkulertMånedligBeløp, valutakode)
}

fun Valutakurs?.tilKronerPerValutaenhet(): KronerPerValutaenhet? {
    val kurs = this?.kurs
    val valutakode = this?.valutakode
    if (kurs == null || valutakode == null) {
        return null
    }

    return KronerPerValutaenhet(kurs, valutakode)
}
