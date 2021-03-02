package no.nav.familie.ba.sak.totrinnskontroll

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.vedtak.Beslutning
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.totrinnskontroll.domene.Totrinnskontroll
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class TotrinnskontrollService(private val behandlingService: BehandlingService,
                              private val totrinnskontrollRepository: TotrinnskontrollRepository) {

    fun hentAktivForBehandling(behandlingId: Long): Totrinnskontroll? {
        return totrinnskontrollRepository.findByBehandlingAndAktiv(behandlingId)
    }

    fun opprettTotrinnskontrollMedSaksbehandler(behandling: Behandling,
                                                saksbehandler: String = SikkerhetContext.hentSaksbehandlerNavn(),
                                                saksbehandlerId: String = SikkerhetContext.hentSaksbehandler()): Totrinnskontroll {
        return lagreOgDeaktiverGammel(Totrinnskontroll(
                behandling = behandling,
                saksbehandler = saksbehandler,
                saksbehandlerId = saksbehandlerId,
        ))
    }

    fun besluttTotrinnskontroll(behandling: Behandling, beslutter: String, beslutterId: String, beslutning: Beslutning) {
        val totrinnskontroll = hentAktivForBehandling(behandlingId = behandling.id)
                               ?: throw Feil(message = "Kan ikke beslutte et vedtak som ikke er sendt til beslutter")

        totrinnskontroll.beslutter = beslutter
        totrinnskontroll.beslutterId = beslutterId
        totrinnskontroll.godkjent = beslutning.erGodkjent()
        if (totrinnskontroll.erUgyldig()) {
            // TODO avklare feilmelding
            throw FunksjonellFeil(
                    melding = "Samme saksbehandler kan ikke foreslå og beslutte iverksetting på samme vedtak",
                    frontendFeilmelding = "Du kan ikke godkjenne ditt eget vedtak")
        }

        lagreEllerOppdater(totrinnskontroll)

        behandlingService.oppdaterStatusPåBehandling(
                behandlingId = behandling.id,
                status = if (beslutning.erGodkjent()) BehandlingStatus.IVERKSETTER_VEDTAK else BehandlingStatus.UTREDES)

    }

    fun opprettAutomatiskTotrinnskontroll(behandling: Behandling) {
        lagreOgDeaktiverGammel(Totrinnskontroll(
                behandling = behandling,
                godkjent = true,
                saksbehandler = SikkerhetContext.hentSaksbehandlerNavn(),
                saksbehandlerId = SikkerhetContext.hentSaksbehandler(),
                beslutter = SikkerhetContext.hentSaksbehandlerNavn(),
                beslutterId = SikkerhetContext.hentSaksbehandler(),
        ))
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
