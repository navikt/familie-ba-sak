package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.KONTAKT_TEAMET_SUFFIX
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag

fun Utbetalingsoppdrag.validerNullutbetaling(
    behandlingskategori: BehandlingKategori,
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
) {
    if (this.utbetalingsperiode.isEmpty() && !kanHaNullutbetaling(behandlingskategori, andelerTilkjentYtelse)) {
        throw FunksjonellFeil(
            "Utbetalingsoppdraget inneholder ingen utbetalingsperioder " +
                "og det er grunn til å tro at denne ikke bør simuleres eller iverksettes. $KONTAKT_TEAMET_SUFFIX",
        )
    }
}

private fun kanHaNullutbetaling(
    behandlingskategori: BehandlingKategori,
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
) = behandlingskategori == BehandlingKategori.EØS &&
    andelerTilkjentYtelse.any { it.erAndelSomharNullutbetalingPgaDifferanseberegning() }
