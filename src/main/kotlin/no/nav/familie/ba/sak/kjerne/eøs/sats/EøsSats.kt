package no.nav.familie.ba.sak.kjerne.eøs.sats

import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.Intervall
import java.math.BigDecimal
import java.time.YearMonth

/**
 * Representerer en EØS-sats fra et bestemt land i landets valuta og utbetalingsintervall.
 *
 * Beløpet angir det utenlandske periodebeløpet som utbetales fra [land],
 * i valutaen [valuta] og med hyppigheten [intervall].
 *
 * [fom] er første måned satsen gjelder.
 * [tom] er siste måned satsen gjelder. Null betyr løpende (ingen kjent sluttdato).
 */
data class EøsSats(
    val land: String,
    val valuta: String,
    val beløp: BigDecimal,
    val fom: YearMonth,
    val tom: YearMonth? = null,
    val intervall: Intervall = Intervall.MÅNEDLIG,
) {
    fun erGyldigForMåned(måned: YearMonth): Boolean = måned >= fom && (tom == null || måned <= tom)
}
