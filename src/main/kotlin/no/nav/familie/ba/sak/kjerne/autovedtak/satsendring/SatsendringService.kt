package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.SatsService.sisteTilleggOrdinærSats
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.ba.sak.task.SatsendringTask
import no.nav.familie.ba.sak.task.erHverdag
import no.nav.familie.leader.LeaderClient
import no.nav.familie.prosessering.error.RekjørSenereException
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.YearMonth

@Service
class SatsendringService(
    private val taskRepository: TaskRepositoryWrapper,
    private val behandlingRepository: BehandlingRepository,
    private val autovedtakService: AutovedtakService,
    private val stegService: StegService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val tilbakestillBehandlingService: TilbakestillBehandlingService
) {

    /**
     * Forsøk å opprett tasker for behandlinger som har gammel sats hver morgen i hele januar.
     * Dette gjør vi for å plukke opp eventuelle førstegangsbehandlinger som blir iverksatt med gammel sats.
     */
    @Scheduled(cron = "0 0 7 * * *")
    fun scheduledFinnOgOpprettTaskerForSatsendring() {
        if (LeaderClient.isLeader() == false) return

        // Vi ønsker kun å opprette tasker i inneværende satsendringsmåned som nå er januar 2022
        if (YearMonth.now() != YearMonth.of(2022, 1)) {
            logger.info("Dropper å lage satsendringsbehandlinger fordi måneden vi er i er ikke en satsendringsmåned")
        } else if (!LocalDateTime.now().erHverdag(0)) {
            logger.info("Dropper å lage satsendringsbehandlinger fordi det ikke er hverdag")
        } else {
            finnOgOpprettTaskerForSatsendring(1654)
        }
    }

    @Transactional
    fun finnOgOpprettTaskerForSatsendring(gammelSats: Int) {
        finnBehandlingerForSatsendring(gammelSats, YearMonth.now()).forEach {
            taskRepository.save(SatsendringTask.opprettTask(it))
        }
    }

    /**
     * Finner behandlinger som trenger satsendring.
     * Se https://github.com/navikt/familie-ba-sak/pull/1361 for eksempel på scheduler.
     *
     * Obs! Denne utplukkingen plukker ut siste iverksatte behandling.
     * Siden den siste iverksatte ikke nødvendigvis er den aktive kan det være
     * åpne behandlinger på fagsaken det kjøres satsendring for. Dette skal bli håndtert i kjøringen
     * av satsendringsbehandlingen.
     */
    fun finnBehandlingerForSatsendring(
        gammelSats: Int,
        satsendringMåned: YearMonth
    ): List<Long> {
        val behandlinger = behandlingRepository.finnBehandlingerForSatsendring(
            iverksatteLøpende = behandlingRepository.finnSisteIverksatteBehandlingFraLøpendeFagsaker(),
            gammelSats = gammelSats,
            månedÅrForEndring = satsendringMåned
        )

        logger.info("Oppretter ${behandlinger.size} tasker på saker som trenger satsendring.")

        return behandlinger
    }

    /**
     * Gjennomfører og commiter revurderingsbehandling
     * med årsak satsendring og uten endring i vilkår.
     *
     */
    @Transactional
    fun utførSatsendring(sistIverksatteBehandlingId: Long) {

        val behandling = behandlingRepository.finnBehandling(behandlingId = sistIverksatteBehandlingId)
        val aktivOgÅpenBehandling = behandlingRepository.findByFagsakAndAktivAndOpen(fagsakId = behandling.fagsak.id)
        val søkerAktør = behandling.fagsak.aktør

        logger.info("Kjører satsendring på $behandling")
        secureLogger.info("Kjører satsendring på $behandling for ${søkerAktør.aktivFødselsnummer()}")
        if (behandling.fagsak.status != FagsakStatus.LØPENDE) throw Feil("Forsøker å utføre satsendring på ikke løpende fagsak ${behandling.fagsak.id}")

        if (aktivOgÅpenBehandling != null) {
            if (aktivOgÅpenBehandling.status.erLåstMenIkkeAvsluttet()) {
                val behandlingLåstMelding =
                    "Behandling $aktivOgÅpenBehandling er låst for endringer og satsendring vil bli forsøkt rekjørt neste dag."
                logger.info(behandlingLåstMelding)
                throw RekjørSenereException(
                    triggerTid = LocalDateTime.now().plusDays(1),
                    årsak = behandlingLåstMelding
                )
            } else if (aktivOgÅpenBehandling.steg.rekkefølge > StegType.VILKÅRSVURDERING.rekkefølge) {
                if (harAlleredeNySats(behandlingId = aktivOgÅpenBehandling.id)) {
                    logger.info("Åpen behandling har allerede siste sats og vi lar den ligge.")
                } else {
                    tilbakestillBehandlingService.tilbakestillBehandlingTilVilkårsvurdering(aktivOgÅpenBehandling)
                    logger.info("Tilbakestiller behandling $aktivOgÅpenBehandling til vilkårsvurderingen")
                }
            } else {
                logger.info("Behandling $aktivOgÅpenBehandling er under utredning, men er allerede i riktig tilstand.")
            }

            return
        }

        val behandlingEtterBehandlingsresultat =
            autovedtakService.opprettAutomatiskBehandlingOgKjørTilBehandlingsresultat(
                fagsak = behandling.fagsak,
                behandlingType = BehandlingType.REVURDERING,
                behandlingÅrsak = BehandlingÅrsak.SATSENDRING
            )

        val opprettetVedtak =
            autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(
                behandlingEtterBehandlingsresultat
            )

        val task = IverksettMotOppdragTask.opprettTask(
            behandlingEtterBehandlingsresultat,
            opprettetVedtak,
            SikkerhetContext.hentSaksbehandler()
        )
        taskRepository.save(task)
    }

    private fun harAlleredeNySats(behandlingId: Long): Boolean {
        val andeler =
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = behandlingId)

        return andeler.any {
            it.sats == sisteTilleggOrdinærSats.beløp
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(SatsendringService::class.java)
        val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}
