package no.nav.familie.ba.sak.totrinnskontroll

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.vedtak.Beslutning
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext.SYSTEM_FORKORTELSE
import no.nav.familie.ba.sak.totrinnskontroll.domene.Totrinnskontroll
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class TotrinnskontrollService(private val behandlingService: BehandlingService,
                              private val totrinnskontrollRepository: TotrinnskontrollRepository) {

    fun hentAktivForBehandling(behandlingId: Long): Totrinnskontroll? {
        return totrinnskontrollRepository.findByBehandlingAndAktiv(behandlingId)
    }

    fun opprettTotrinnskontroll(behandling: Behandling) {
        lagreOgDeaktiverGammel(Totrinnskontroll(
                behandling = behandling,
                saksbehandler = SikkerhetContext.hentSaksbehandlerNavn()
        ))
    }

    fun besluttTotrinnskontroll(behandling: Behandling, beslutter: String, beslutning: Beslutning) {
        val totrinnskontroll = hentAktivForBehandling(behandlingId = behandling.id)
                               ?: throw Feil(message = "Kan ikke beslutte et vedtak som ikke er sendt til beslutter")

        if (totrinnskontroll.saksbehandler == beslutter) {
            error("Samme saksbehandler kan ikke foreslå og beslutte om iverksetting på samme vedtak")
        }

        behandlingService.oppdaterStatusPåBehandling(
                behandlingId = behandling.id,
                status = if (beslutning.erGodkjent()) BehandlingStatus.GODKJENT else BehandlingStatus.UNDERKJENT_AV_BESLUTTER)

        totrinnskontroll.beslutter = beslutter
        totrinnskontroll.godkjent = beslutning.erGodkjent()
        lagreEllerOppdater(totrinnskontroll)
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
