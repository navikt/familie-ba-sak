package no.nav.familie.ba.sak.dokument

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.totrinnskontroll.domene.Totrinnskontroll

object DokumentUtils {

    fun hentSaksbehandlerOgBeslutter(behandling: Behandling, totrinnskontroll: Totrinnskontroll?): Pair<String, String> {
        return when {
            behandling.steg <= StegType.SEND_TIL_BESLUTTER || totrinnskontroll == null -> {
                Pair(SikkerhetContext.hentSaksbehandlerNavn(), "Beslutter")
            }
            totrinnskontroll.erBesluttet() -> {
                Pair(totrinnskontroll.saksbehandler, totrinnskontroll.beslutter!!)
            }
            behandling.steg == StegType.BESLUTTE_VEDTAK -> {
                Pair(totrinnskontroll.saksbehandler,
                     if (totrinnskontroll.saksbehandler == SikkerhetContext.hentSaksbehandlerNavn()) "Beslutter" else SikkerhetContext.hentSaksbehandlerNavn())
            }
            else -> {
                throw Feil("Prøver å hente saksbehandler og beslutters navn for generering av brev i en ukjent tilstand.")
            }
        }
    }
}