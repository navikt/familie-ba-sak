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
    val beløp = this?.kalkulertMånedligBeløp ?: return null
    val valutakode = this.valutakode ?: return null
    return Valutabeløp(beløp, valutakode)
}

fun Valutakurs?.tilKronerPerValutaenhet(): KronerPerValutaenhet? {
    val kurs = this?.kurs ?: return null
    val valutakode = this.valutakode ?: return null
    return KronerPerValutaenhet(kurs, valutakode)
}
