package no.nav.familie.ba.sak.totrinnskontroll

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.vedtak.Beslutning
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext.SYSTEM_NAVN
import no.nav.familie.ba.sak.totrinnskontroll.domene.Totrinnskontroll
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class TotrinnskontrollService(private val behandlingService: BehandlingService,
                              private val totrinnskontrollRepository: TotrinnskontrollRepository) {

    fun hentAktivForBehandling(behandlingId: Long): Totrinnskontroll? {
        return totrinnskontrollRepository.findByBehandlingAndAktiv(behandlingId)
    }

    fun opprettEllerHentTotrinnskontroll(behandling: Behandling,
                                         saksbehandler: String = SikkerhetContext.hentSaksbehandlerNavn()): Totrinnskontroll {
        return when (val totrinnskontroll = totrinnskontrollRepository.findByBehandlingAndAktiv(behandlingId = behandling.id)) {
            null -> lagreOgDeaktiverGammel(Totrinnskontroll(
                    behandling = behandling,
                    saksbehandler = saksbehandler
            ))
            else -> {
                if (totrinnskontroll.saksbehandler != saksbehandler) {
                    lagreOgDeaktiverGammel(Totrinnskontroll(
                            behandling = behandling,
                            saksbehandler = saksbehandler
                    ))
                } else {
                    totrinnskontroll
                }
            }
        }
    }

    fun besluttTotrinnskontroll(behandling: Behandling, beslutter: String, beslutning: Beslutning) {
        val totrinnskontroll = hentAktivForBehandling(behandlingId = behandling.id)
                               ?: throw Feil(message = "Kan ikke beslutte et vedtak som ikke er sendt til beslutter")

        totrinnskontroll.beslutter = beslutter
        totrinnskontroll.godkjent = beslutning.erGodkjent()
        if (erTotrinnskontrollGyldig(totrinnskontroll)) {
            error("Samme saksbehandler kan ikke foreslå og beslutte iverksetting på samme vedtak")
        }

        behandlingService.oppdaterStatusPåBehandling(
                behandlingId = behandling.id,
                status = if (beslutning.erGodkjent()) BehandlingStatus.GODKJENT else BehandlingStatus.UNDERKJENT_AV_BESLUTTER)

        lagreEllerOppdater(totrinnskontroll)
    }

    fun erTotrinnskontrollGyldig(totrinnskontroll: Totrinnskontroll): Boolean {
        return totrinnskontroll.saksbehandler == totrinnskontroll.beslutter &&
               !(totrinnskontroll.saksbehandler == SYSTEM_NAVN && totrinnskontroll.beslutter == SYSTEM_NAVN)
    }

    fun lagreOgDeaktiverGammel(totrinnskontroll: Totrinnskontroll): Totrinnskontroll {
        val aktivTotrinnskontroll = hentAktivForBehandling(totrinnskontroll.behandling.id)

        if (aktivTotrinnskontroll != null && aktivTotrinnskontroll.id != totrinnskontroll.id) {
            totrinnskontrollRepository.saveAndFlush(aktivTotrinnskontroll.also { it.aktiv = false })
        }

        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppretter totrinnskontroll $totrinnskontroll")
        return totrinnskontrollRepository.save(totrinnskontroll)
    }

    fun lagreEllerOppdater(totrinnskontroll: Totrinnskontroll): Totrinnskontroll {
        return totrinnskontrollRepository.save(totrinnskontroll)
    }

    companion object {
        val LOG = LoggerFactory.getLogger(this::class.java)
    }
}
