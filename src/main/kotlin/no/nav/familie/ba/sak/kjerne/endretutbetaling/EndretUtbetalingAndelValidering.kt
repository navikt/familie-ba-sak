package no.nav.familie.ba.sak.kjerne.endretutbetaling

import no.nav.familie.ba.sak.common.UtbetalingsikkerhetFeil
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel

object EndretUtbetalingAndelValidering {

    fun validerIngenOverlappendeEndring(
        endretUtbetalingAndel: EndretUtbetalingAndel,
        eksisterendeEndringerPåBehandling: List<EndretUtbetalingAndel>
    ) {

        endretUtbetalingAndel.validerUtfyltEndring()
        if (eksisterendeEndringerPåBehandling.any {
                it.overlapperMed(endretUtbetalingAndel.periode()) &&
                    it.person == endretUtbetalingAndel.person &&
                    it.årsak == endretUtbetalingAndel.årsak
            }
        ) {
            throw UtbetalingsikkerhetFeil(
                melding = "Perioden som blir forsøkt lagt til overlapper med eksisterende periode på person.",
                frontendFeilmelding = "Perioden du forsøker å legge til overlapper med eksisterende periode på personen. Om dette er ønskelig må du først endre den eksisterende perioden."
            )
        }
    }

    fun validerPeriodeInnenforTilkjentytelse(
        endretUtbetalingAndel: EndretUtbetalingAndel,
        andelTilkjentYtelser: List<AndelTilkjentYtelse>
    ) {

        endretUtbetalingAndel.validerUtfyltEndring()
        val minsteDatoForTilkjentYtelse = andelTilkjentYtelser.filter {
            it.personIdent == endretUtbetalingAndel.person!!.personIdent.ident
        }.minByOrNull { it.stønadFom }?.stønadFom

        val størsteDatoForTilkjentYtelse = andelTilkjentYtelser.filter {
            it.personIdent == endretUtbetalingAndel.person!!.personIdent.ident
        }.maxByOrNull { it.stønadTom }?.stønadTom

        if ((minsteDatoForTilkjentYtelse == null || størsteDatoForTilkjentYtelse == null) ||
            (
                endretUtbetalingAndel.fom!!.isBefore(minsteDatoForTilkjentYtelse) ||
                    endretUtbetalingAndel.tom!!.isAfter(størsteDatoForTilkjentYtelse)
                )
        ) {
            throw UtbetalingsikkerhetFeil(
                melding = "Det er ingen tilkjent ytelse for personen det blir forsøkt lagt til en endret periode for.",
                frontendFeilmelding = "Du har valgt en periode der det ikke finnes tilkjent ytelse for valgt person i hele eller deler av perioden."
            )
        }
    }
}
