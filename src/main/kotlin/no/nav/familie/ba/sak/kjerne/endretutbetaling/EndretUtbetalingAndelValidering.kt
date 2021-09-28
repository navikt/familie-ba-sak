package no.nav.familie.ba.sak.kjerne.endretutbetaling

import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.UtbetalingsikkerhetFeil
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel

object EndretUtbetalingAndelValidering {

    fun validerIngenOverlappendeEndring(endretUtbetalingAndel: EndretUtbetalingAndel,
                                        eksisterendeEndringerPåBehandling: List<EndretUtbetalingAndel>) {

        if (eksisterendeEndringerPåBehandling.any {
                    it.overlapperMed(MånedPeriode(endretUtbetalingAndel.fom, endretUtbetalingAndel.tom)) &&
                    it.person == endretUtbetalingAndel.person &&
                    it.årsak == endretUtbetalingAndel.årsak
        }) {
            throw UtbetalingsikkerhetFeil(melding = "Perioden som forsøkes lagt til overlapper med eksisterende periode gjeldende samme årsak og person.")
        }

    }

    fun validerPeriodeInnenforTilkjentytelse() {

    }

    fun validerAtDetIkkeUtbetalesForMyePåBarn() {
        // TODO: Vurdere om TilkjentYtelseValidering.validerAtBarnIkkeFårFlereUtbetalingerSammePeriode kan brukes.
    }
}