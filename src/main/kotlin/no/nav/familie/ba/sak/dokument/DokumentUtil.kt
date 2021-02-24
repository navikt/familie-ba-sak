package no.nav.familie.ba.sak.dokument

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.domene.BehandlingType

fun vedtaksbrevToggelNavnSuffix(behandling: Behandling): String {
    return if (behandling.skalBehandlesAutomatisk) {
        DokumentService.Companion.BrevToggleSuffix.IKKE_STØTTET.suffix
    } else when (behandling.type) {
        BehandlingType.FØRSTEGANGSBEHANDLING -> when (behandling.resultat) {
            BehandlingResultat.INNVILGET, BehandlingResultat.INNVILGET_OG_OPPHØRT, BehandlingResultat.DELVIS_INNVILGET -> DokumentService.Companion.BrevToggleSuffix.FØRSTEGANGSBEHANDLING.suffix
            else -> DokumentService.Companion.BrevToggleSuffix.IKKE_STØTTET.suffix
        }
        BehandlingType.REVURDERING -> when (behandling.resultat) {
            BehandlingResultat.INNVILGET, BehandlingResultat.DELVIS_INNVILGET -> DokumentService.Companion.BrevToggleSuffix.VEDTAK_ENDRING.suffix
            BehandlingResultat.OPPHØRT -> DokumentService.Companion.BrevToggleSuffix.OPPHØR.suffix
            BehandlingResultat.INNVILGET_OG_OPPHØRT, BehandlingResultat.ENDRET_OG_OPPHØRT -> DokumentService.Companion.BrevToggleSuffix.OPPHØR_MED_ENDRING.suffix
            else -> DokumentService.Companion.BrevToggleSuffix.IKKE_STØTTET.suffix
        }
        else -> DokumentService.Companion.BrevToggleSuffix.IKKE_STØTTET.suffix
    }
}