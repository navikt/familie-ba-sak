package no.nav.familie.ba.sak.kjerne.eøs.sats

import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.Intervall
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtfyltUtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.filtrerErUtfylt
import java.math.BigDecimal
import java.time.YearMonth

/**
 * Representerer en sats fra et bestemt EØS-land i landets valuta og utbetalingsintervall.
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

    override fun toString() = "${javaClass.simpleName}(land='$land', valuta='$valuta', beløp=$beløp, fom=${fom.tilKortString()}, tom=${tom?.tilKortString()}, intervall=$intervall)"
}

fun UtfyltUtenlandskPeriodebeløp.overlapper(eøsSats: EøsSats): Boolean =
    (this.tom == null || eøsSats.fom <= this.tom) &&
        (eøsSats.tom == null || this.fom <= eøsSats.tom)

/**
 * Filtrerer ut [UtenlandskPeriodebeløp] som er relevante for [eøsSats] — dvs. er utfylt,
 * gjelder samme utbetalingsland som [eøsSats], og overlapper perioden [eøsSats] gjelder for.
 */
fun Collection<UtenlandskPeriodebeløp>.filtrerErRelevantForSats(eøsSats: EøsSats) = filtrerErUtfylt().filter { it.utbetalingsland == eøsSats.land && it.overlapper(eøsSats) }
