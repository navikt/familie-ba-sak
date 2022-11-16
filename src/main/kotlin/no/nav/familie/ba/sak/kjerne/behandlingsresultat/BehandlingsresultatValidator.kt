package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak

internal fun validerBehandlingsresultat(behandling: Behandling, resultat: Behandlingsresultat) {
    if ((
        behandling.type == BehandlingType.FØRSTEGANGSBEHANDLING && setOf(
                Behandlingsresultat.AVSLÅTT_OG_OPPHØRT,
                Behandlingsresultat.ENDRET_UTBETALING,
                Behandlingsresultat.ENDRET_UTEN_UTBETALING,
                Behandlingsresultat.ENDRET_OG_OPPHØRT,
                Behandlingsresultat.OPPHØRT,
                Behandlingsresultat.FORTSATT_INNVILGET,
                Behandlingsresultat.IKKE_VURDERT
            ).contains(resultat)
        ) ||
        (behandling.type == BehandlingType.REVURDERING && resultat == Behandlingsresultat.IKKE_VURDERT)
    ) {
        val feilmelding = "Behandlingsresultatet ${resultat.displayName.lowercase()} " +
            "er ugyldig i kombinasjon med behandlingstype '${behandling.type.visningsnavn}'."
        throw FunksjonellFeil(frontendFeilmelding = feilmelding, melding = feilmelding)
    }
    if (behandling.opprettetÅrsak == BehandlingÅrsak.KLAGE && setOf(
            Behandlingsresultat.AVSLÅTT_OG_OPPHØRT,
            Behandlingsresultat.AVSLÅTT_ENDRET_OG_OPPHØRT,
            Behandlingsresultat.AVSLÅTT_OG_ENDRET,
            Behandlingsresultat.AVSLÅTT
        ).contains(resultat)
    ) {
        val feilmelding = "Behandlingsårsak ${behandling.opprettetÅrsak.visningsnavn.lowercase()} " +
            "er ugyldig i kombinasjon med resultat '${resultat.displayName.lowercase()}'."
        throw FunksjonellFeil(frontendFeilmelding = feilmelding, melding = feilmelding)
    }
}

internal fun validerYtelsePersoner(ytelsePersoner: List<YtelsePerson>) {
    if (ytelsePersoner.flatMap { it.resultater }.any { it == YtelsePersonResultat.IKKE_VURDERT }) {
        throw Feil(message = "Minst én ytelseperson er ikke vurdert")
    }

    if (ytelsePersoner.any { it.ytelseSlutt == null }) {
        throw Feil(message = "YtelseSlutt ikke satt ved utledning av behandlingsresultat")
    }

    if (ytelsePersoner.any {
        it.resultater.contains(YtelsePersonResultat.OPPHØRT) && it.ytelseSlutt?.isAfter(
                inneværendeMåned()
            ) == true
    }
    ) {
        throw Feil(message = "Minst én ytelseperson har fått opphør som resultat og ytelseSlutt etter inneværende måned")
    }
}
