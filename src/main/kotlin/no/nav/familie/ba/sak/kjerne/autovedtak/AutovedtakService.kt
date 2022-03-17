package no.nav.familie.ba.sak.kjerne.autovedtak

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

interface AutovedtakBehandlingService<Behandlingsdata> {
    fun kjørBehandling(behandlingsdata: Behandlingsdata): String
}

enum class Autovedtaktype(val displayName: String) {
    FØDSELSHENDELSE("Fødselshendelse"),
    SMÅBARNSTILLEGG("Småbarnstillegg"),
    OMREGNING_BREV("Omregning")
}

@Service
class AutovedtakService(
    private val stegService: StegService,
    private val behandlingService: BehandlingService,
    private val vedtakService: VedtakService,
    private val loggService: LoggService,
    private val totrinnskontrollService: TotrinnskontrollService,
    private val opprettTaskService: OpprettTaskService,
    private val fagsakService: FagsakService
) {
    private val antallAutovedtakÅpenBehandling: Map<Autovedtaktype, Counter> = Autovedtaktype.values().associateWith {
        Metrics.counter("behandling.saksbehandling.autovedtak.aapen_behandling", "type", it.name)
    }

    fun håndterÅpenBehandlingOgAvbrytAutovedtak(aktør: Aktør, autovedtaktype: Autovedtaktype): Boolean {
        val åpenBehandling = fagsakService.hent(aktør)?.let {
            behandlingService.hentAktivOgÅpenForFagsak(it.id)
        }

        return if (åpenBehandling != null) {
            antallAutovedtakÅpenBehandling[autovedtaktype]?.increment()

            opprettOppgaveForManuellBehandling(
                behandling = åpenBehandling,
                begrunnelse = "${autovedtaktype.displayName}: Bruker har åpen behandling",
                oppgavetype = Oppgavetype.VurderLivshendelse
            )

            true
        } else false
    }

    fun opprettAutomatiskBehandlingOgKjørTilBehandlingsresultat(
        aktør: Aktør,
        behandlingType: BehandlingType,
        behandlingÅrsak: BehandlingÅrsak,
    ): Behandling {
        val nyBehandling = stegService.håndterNyBehandling(
            NyBehandling(
                behandlingType = behandlingType,
                behandlingÅrsak = behandlingÅrsak,
                søkersIdent = aktør.aktivFødselsnummer(),
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

    fun omgjørBehandlingTilManuellOgKjørSteg(behandling: Behandling, steg: StegType): Behandling {
        val omgjortBehandling = behandlingService.omgjørTilManuellBehandling(behandling)

        return when (steg) {
            StegType.VILKÅRSVURDERING -> stegService.håndterVilkårsvurdering(omgjortBehandling)
            else -> throw Feil("Steg $steg er ikke støttet ved omgjøring av automatisk behandling til manuell.")
        }
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
