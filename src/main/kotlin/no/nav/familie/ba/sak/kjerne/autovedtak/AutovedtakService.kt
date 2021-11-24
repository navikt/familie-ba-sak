package no.nav.familie.ba.sak.kjerne.autovedtak

import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AutovedtakService(
    private val opprettTaskService: OpprettTaskService,
    private val loggService: LoggService,
    private val totrinnskontrollService: TotrinnskontrollService,
    private val vedtakService: VedtakService,
    private val stegService: StegService
) {

    fun opprettAutomatiskBehandlingOgKjørTilBehandlingsresultat(
        fagsak: Fagsak,
        behandlingType: BehandlingType,
        behandlingÅrsak: BehandlingÅrsak,
    ): Behandling {
        val nyBehandling = stegService.håndterNyBehandling(
            NyBehandling(
                behandlingType = behandlingType,
                behandlingÅrsak = behandlingÅrsak,
                søkersIdent = fagsak.hentAktivIdent().ident,
                skalBehandlesAutomatisk = true
            )
        )

        val behandlingEtterBehandlingsresultat = stegService.håndterVilkårsvurdering(nyBehandling)
        return behandlingEtterBehandlingsresultat
    }

    fun opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(behandling: Behandling): Vedtak {
        totrinnskontrollService.opprettAutomatiskTotrinnskontroll(behandling)
        loggService.opprettBeslutningOmVedtakLogg(behandling, Beslutning.GODKJENT)

        val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandling.id)
            ?: error("Fant ikke aktivt vedtak på behandling ${behandling.id}")
        return vedtakService.oppdaterVedtakMedStønadsbrev(vedtak = vedtak)
    }

    fun opprettOppgaveForManuellBehandling(
        behandling: Behandling,
        oppgavetype: Oppgavetype,
        begrunnelse: String = "",
        opprettLogginnslag: Boolean = false
    ): String {
        logger.info("Sender autovedtak til manuell behandling, se secureLogger for mer detaljer.")
        secureLogger.info("Sender autovedtak til manuell behandling. Begrunnelse: $begrunnelse")
        opprettTaskService.opprettOppgaveTask(
            behandlingId = behandling.id,
            oppgavetype = oppgavetype,
            beskrivelse = begrunnelse
        )

        if (opprettLogginnslag) {
            loggService.opprettAutovedtakTilManuellBehandling(
                behandling = behandling,
                tekst = begrunnelse
            )
        }

        return begrunnelse
    }

    companion object {
        val logger = LoggerFactory.getLogger(AutovedtakService::class.java)
        val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}