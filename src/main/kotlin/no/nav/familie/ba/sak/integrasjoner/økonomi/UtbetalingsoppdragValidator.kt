package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.KONTAKT_TEAMET_SUFFIX
import no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag.harLøpendeUtbetaling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag

fun Utbetalingsoppdrag.valider(
    behandlingsresultat: Behandlingsresultat,
    erEndreMigreringsdatoBehandling: Boolean = false,
    erOkMedManglendeUtbetalingsperioder: Boolean = false
) {
    if (this.utbetalingsperiode.isNotEmpty() && behandlingsresultat == Behandlingsresultat.FORTSATT_INNVILGET && !erEndreMigreringsdatoBehandling) {
        throw FunksjonellFeil("Behandling har resultat fortsatt innvilget, men det finnes utbetalingsperioder som ifølge systemet skal endres. $KONTAKT_TEAMET_SUFFIX")
    } else if (this.utbetalingsperiode.isEmpty() && !erOkMedManglendeUtbetalingsperioder) {
        throw FunksjonellFeil(
            "Utbetalingsoppdraget inneholder ingen utbetalingsperioder " +
                "og det er grunn til å tro at denne ikke bør simuleres eller iverksettes. $KONTAKT_TEAMET_SUFFIX"
        )
    }
}

// nyValideringsmetode med logikk for å slippe nullutbetaling saker
fun Utbetalingsoppdrag.valider(
    behandlingsresultat: Behandlingsresultat,
    behandlingskategori: BehandlingKategori,
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    erEndreMigreringsdatoBehandling: Boolean = false
) {
    if (this.utbetalingsperiode.isNotEmpty() && behandlingsresultat == Behandlingsresultat.FORTSATT_INNVILGET && !erEndreMigreringsdatoBehandling) {
        throw FunksjonellFeil("Behandling har resultat fortsatt innvilget, men det finnes utbetalingsperioder som ifølge systemet skal endres. $KONTAKT_TEAMET_SUFFIX")
    } else if (this.utbetalingsperiode.isEmpty() &&
        !kanHaNullutbetaling(behandlingskategori, andelerTilkjentYtelse)
    ) {
        throw FunksjonellFeil(
            "Utbetalingsoppdraget inneholder ingen utbetalingsperioder " +
                "og det er grunn til å tro at denne ikke bør simuleres eller iverksettes. $KONTAKT_TEAMET_SUFFIX"
        )
    }
}

fun Utbetalingsoppdrag.validerOpphørsoppdrag() {
    if (this.harLøpendeUtbetaling()) {
        error("Generert utbetalingsoppdrag for opphør inneholder oppdragsperioder med løpende utbetaling.")
    }

    if (this.utbetalingsperiode.isNotEmpty() && this.utbetalingsperiode.none { it.opphør != null }) {
        error("Generert utbetalingsoppdrag for opphør mangler opphørsperioder.")
    }
}

private fun kanHaNullutbetaling(
    behandlingskategori: BehandlingKategori,
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>
) = behandlingskategori == BehandlingKategori.EØS &&
    andelerTilkjentYtelse.any { it.erAndelSomharNullutbetaling() }
