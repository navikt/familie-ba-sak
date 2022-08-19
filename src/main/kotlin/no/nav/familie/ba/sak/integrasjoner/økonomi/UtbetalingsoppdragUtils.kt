package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.KONTAKT_TEAMET_SUFFIX
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
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

fun Utbetalingsoppdrag.validerForNullutbetaling(
    behandlingsresultat: Behandlingsresultat,
    behandlingskategori: BehandlingKategori,
    erEndreMigreringsdatoBehandling: Boolean = false
) {
    if (this.utbetalingsperiode.isNotEmpty() && behandlingsresultat == Behandlingsresultat.FORTSATT_INNVILGET && !erEndreMigreringsdatoBehandling) {
        throw FunksjonellFeil("Behandling har resultat fortsatt innvilget, men det finnes utbetalingsperioder som ifølge systemet skal endres. $KONTAKT_TEAMET_SUFFIX")
    } else if (this.utbetalingsperiode.isEmpty() && !kanHaNullutbetaling(behandlingsresultat, behandlingskategori)) {
        throw FunksjonellFeil(
            "Utbetalingsoppdraget inneholder ingen utbetalingsperioder " +
                "og det er grunn til å tro at denne ikke bør simuleres eller iverksettes. $KONTAKT_TEAMET_SUFFIX"
        )
    }
}

private fun kanHaNullutbetaling(
    behandlingsresultat: Behandlingsresultat,
    behandlingskategori: BehandlingKategori
) = behandlingskategori == BehandlingKategori.EØS && behandlingsresultat in setOf(
    Behandlingsresultat.INNVILGET,
    Behandlingsresultat.FORTSATT_INNVILGET,
    Behandlingsresultat.DELVIS_INNVILGET,
    Behandlingsresultat.INNVILGET_OG_ENDRET,
    Behandlingsresultat.DELVIS_INNVILGET_OG_ENDRET
)
