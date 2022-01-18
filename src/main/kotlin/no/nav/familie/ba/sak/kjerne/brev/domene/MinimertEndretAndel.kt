package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.NullableMånedPeriode
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.overlapperHeltEllerDelvisMed
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import java.math.BigDecimal
import java.time.YearMonth

interface IMinimertEndretAndel {
    var fom: YearMonth?
    var tom: YearMonth?
    var årsak: Årsak?
    var prosent: BigDecimal?
}

class MinimertEndretAndel(
    val aktørId: String,
    override var fom: YearMonth?,
    override var tom: YearMonth?,
    override var årsak: Årsak?,
    override var prosent: BigDecimal?
) : IMinimertEndretAndel {
    fun månedPeriode() = MånedPeriode(fom!!, tom!!)

    fun erOverlappendeMed(nullableMånedPeriode: NullableMånedPeriode): Boolean {
        if (nullableMånedPeriode.fom == null) {
            throw Feil("Fom ble null ved sjekk av overlapp av periode til endretUtbetalingAndel")
        }

        return MånedPeriode(
            this.fom!!,
            this.tom!!,
        ).overlapperHeltEllerDelvisMed(
            MånedPeriode(
                nullableMånedPeriode.fom,
                nullableMånedPeriode.tom ?: TIDENES_ENDE.toYearMonth()
            )
        )
    }
}

fun List<MinimertEndretAndel>.somOverlapper(nullableMånedPeriode: NullableMånedPeriode) =
    this.filter { it.erOverlappendeMed(nullableMånedPeriode) }

fun EndretUtbetalingAndel.tilMinimertEndretUtbetalingAndel() = MinimertEndretAndel(
    fom = this.fom!!,
    tom = this.tom!!,
    aktørId = this.person?.aktør?.aktørId ?: throw Feil(
        "Finner ikke aktørId på endretUtbetalingsandel ${this.id} " +
            "ved konvertering til minimertEndretUtbetalingsandel"
    ),
    årsak = this.årsak ?: throw Feil(
        "Har ikke årsak på endretUtbetalingsandel ${this.id} " +
            "ved konvertering til minimertEndretUtbetalingsandel"
    ),
    prosent = this.prosent
)
