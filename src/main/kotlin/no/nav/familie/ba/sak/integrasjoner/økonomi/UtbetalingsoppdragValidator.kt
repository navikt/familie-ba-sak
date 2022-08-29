package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.KONTAKT_TEAMET_SUFFIX
import no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag.harLøpendeUtbetaling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag

fun Utbetalingsoppdrag.valider(
    behandlingsresultat: Behandlingsresultat,
    erEndreMigreringsdatoBehandling: Boolean = false
) {
    if (this.utbetalingsperiode.isNotEmpty() && behandlingsresultat == Behandlingsresultat.FORTSATT_INNVILGET && !erEndreMigreringsdatoBehandling) {
        throw FunksjonellFeil("Behandling har resultat fortsatt innvilget, men det finnes utbetalingsperioder som ifølge systemet skal endres. $KONTAKT_TEAMET_SUFFIX")
    } else if (this.utbetalingsperiode.isEmpty()) {
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
    kompetanser: List<Kompetanse>,
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    erEndreMigreringsdatoBehandling: Boolean = false
) {
    if (this.utbetalingsperiode.isNotEmpty() && behandlingsresultat == Behandlingsresultat.FORTSATT_INNVILGET && !erEndreMigreringsdatoBehandling) {
        throw FunksjonellFeil("Behandling har resultat fortsatt innvilget, men det finnes utbetalingsperioder som ifølge systemet skal endres. $KONTAKT_TEAMET_SUFFIX")
    } else if (this.utbetalingsperiode.isEmpty() &&
        !kanHaNullutbetaling(behandlingskategori, kompetanser, andelerTilkjentYtelse)
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

private fun erSekundærlandSak(
    behandlingskategori: BehandlingKategori,
    kompetanser: List<Kompetanse>
) = behandlingskategori == BehandlingKategori.EØS &&
    kompetanser.any { it.resultat == KompetanseResultat.NORGE_ER_SEKUNDÆRLAND }

private fun kanHaNullutbetaling(
    behandlingskategori: BehandlingKategori,
    kompetanser: List<Kompetanse>,
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>
) = erSekundærlandSak(behandlingskategori, kompetanser) &&
    andelerTilkjentYtelse.any { it.erAndelSomharNullutbetaling() }
