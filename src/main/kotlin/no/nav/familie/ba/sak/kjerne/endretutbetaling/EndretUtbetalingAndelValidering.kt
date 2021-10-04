package no.nav.familie.ba.sak.kjerne.endretutbetaling

import no.nav.familie.ba.sak.common.UtbetalingsikkerhetFeil
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel

object EndretUtbetalingAndelValidering {

    fun validerIngenOverlappendeEndring(endretUtbetalingAndel: EndretUtbetalingAndel,
                                        eksisterendeEndringerPåBehandling: List<EndretUtbetalingAndel>) {

        endretUtbetalingAndel.validerUtfyltEndring()
        if (eksisterendeEndringerPåBehandling.any {
                    it.overlapperMed(endretUtbetalingAndel.periode()) &&
                    it.person == endretUtbetalingAndel.person &&
                    it.årsak == endretUtbetalingAndel.årsak
                }) {
            throw UtbetalingsikkerhetFeil(melding = "Perioden som forsøkes lagt til overlapper med eksisterende periode gjeldende samme årsak og person.",
                                          frontendFeilmelding = "Perioden som forsøkes lagt til overlapper med eksisterende periode gjeldende samme årsak og person.")
        }
    }

    fun validerPeriodeInnenforTilkjentytelse(endretUtbetalingAndel: EndretUtbetalingAndel,
                                             andelTilkjentYtelser: List<AndelTilkjentYtelse>) {

        endretUtbetalingAndel.validerUtfyltEndring()
        val minsteDatoForTilkjentYtelse = andelTilkjentYtelser.filter {
            it.personIdent == endretUtbetalingAndel.person!!.personIdent.ident
        }.minByOrNull { it.stønadFom }?.stønadFom

        val størsteDatoForTilkjentYtelse = andelTilkjentYtelser.filter {
            it.personIdent == endretUtbetalingAndel.person!!.personIdent.ident
        }.maxByOrNull { it.stønadTom }?.stønadTom

        if ((minsteDatoForTilkjentYtelse == null || størsteDatoForTilkjentYtelse == null) ||
            (endretUtbetalingAndel.fom!!.isBefore(minsteDatoForTilkjentYtelse) ||
             endretUtbetalingAndel.tom!!.isAfter(størsteDatoForTilkjentYtelse))) {
            throw UtbetalingsikkerhetFeil(melding = "Det er ingen tilkjent ytelse for personen det legges til en endret periode for.",
                                          frontendFeilmelding = "Det er ingen tilkjent ytelse for personen det legges til en endret periode for.")
        }
    }
}