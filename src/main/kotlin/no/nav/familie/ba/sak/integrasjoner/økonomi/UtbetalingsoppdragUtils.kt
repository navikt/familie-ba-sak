package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag

fun Utbetalingsoppdrag.valider(behandlingsresultat: BehandlingResultat) {
    if (this.utbetalingsperiode.isNotEmpty() && behandlingsresultat == BehandlingResultat.FORTSATT_INNVILGET) {
        throw FunksjonellFeil("Behandling har resultat fortsatt innvilget, men det finnes utbetalingsperioder som ifølge systemet skal endres.")
    } else if (this.utbetalingsperiode.isEmpty()) {
        throw FunksjonellFeil(
            "Utbetalingsoppdraget inneholder ingen utbetalingsperioder " +
                "og det er grunn til å tro at denne ikke bør simuleres eller iverksettes. " +
                "Undersøk om dette er riktig."
        )
    }
}