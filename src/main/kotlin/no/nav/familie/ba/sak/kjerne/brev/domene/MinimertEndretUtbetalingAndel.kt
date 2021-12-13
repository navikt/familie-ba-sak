package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.NullableMånedPeriode
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.overlapperHeltEllerDelvisMed
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak

/**
 * NB: Bør ikke brukes internt, men kun ut mot eksterne tjenester siden klassen
 * inneholder personIdent og ikke aktørId.
 */
data class MinimertEndretUtbetalingAndel(
    val periode: MånedPeriode,
    val personIdent: String,
    val årsak: Årsak,
) {
    fun erOverlappendeMed(nullableMånedPeriode: NullableMånedPeriode): Boolean {
        if (nullableMånedPeriode.fom == null) {
            throw Feil("Fom ble null ved sjekk av overlapp av periode til endretUtbetalingAndel")
        }

        return MånedPeriode(
            this.periode.fom,
            this.periode.tom
        ).overlapperHeltEllerDelvisMed(
            MånedPeriode(
                nullableMånedPeriode.fom,
                nullableMånedPeriode.tom ?: TIDENES_ENDE.toYearMonth()
            )
        )
    }
}

fun List<MinimertEndretUtbetalingAndel>.somOverlapper(nullableMånedPeriode: NullableMånedPeriode) =
    this.filter { it.erOverlappendeMed(nullableMånedPeriode) }

fun EndretUtbetalingAndel.tilMinimertEndretUtbetalingAndel() = MinimertEndretUtbetalingAndel(
    periode = this.periode,
    personIdent = this.person?.personIdent?.ident ?: throw Feil(
        "Har ikke ident på endretUtbetalingsandel ${this.id} " +
            "ved konvertering til minimertEndretUtbetalingsandel"
    ),
    årsak = this.årsak ?: throw Feil(
        "Har ikke årsak på endretUtbetalingsandel ${this.id} " +
            "ved konvertering til minimertEndretUtbetalingsandel"
    )
)
