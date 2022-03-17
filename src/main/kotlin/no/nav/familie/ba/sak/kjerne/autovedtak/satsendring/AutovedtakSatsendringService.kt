package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakBehandlingService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.SatsService.sisteTilleggOrdinærSats
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.ba.sak.task.SatsendringTask
import no.nav.familie.ba.sak.task.erHverdag
import no.nav.familie.leader.LeaderClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.YearMonth

@Service
class AutovedtakSatsendringService(
    private val taskRepository: TaskRepositoryWrapper,
    private val behandlingRepository: BehandlingRepository,
    private val autovedtakService: AutovedtakService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val tilbakestillBehandlingService: TilbakestillBehandlingService
) : AutovedtakBehandlingService<Long> {

    /**
     * Gjennomfører og commiter revurderingsbehandling
     * med årsak satsendring og uten endring i vilkår.
     *
     */
    @Transactional
    override fun kjørBehandling(sistIverksatteBehandlingId: Long): String {

        val behandling = behandlingRepository.finnBehandling(behandlingId = sistIverksatteBehandlingId)
        val aktivOgÅpenBehandling = behandlingRepository.findByFagsakAndAktivAndOpen(fagsakId = behandling.fagsak.id)
        val søkerAktør = behandling.fagsak.aktør

        logger.info("Kjører satsendring på $behandling")
        secureLogger.info("Kjører satsendring på $behandling for ${søkerAktør.aktivFødselsnummer()}")
        if (behandling.fagsak.status != FagsakStatus.LØPENDE) throw Feil("Forsøker å utføre satsendring på ikke løpende fagsak ${behandling.fagsak.id}")

        // TODO vurdere å innlemme dette i åpen behandling opplegg?
        if (aktivOgÅpenBehandling != null) {
            val brukerHarÅpenBehandlingMelding = if (harAlleredeNySats(behandlingId = aktivOgÅpenBehandling.id)) {
                "Åpen behandling har allerede siste sats og vi lar den ligge."
            } else if (aktivOgÅpenBehandling.status.erLåstMenIkkeAvsluttet()) {
                "Behandling $aktivOgÅpenBehandling er låst for endringer og satsendring vil bli trigget neste virkedag."
            } else if (aktivOgÅpenBehandling.steg.rekkefølge > StegType.VILKÅRSVURDERING.rekkefølge) {
                tilbakestillBehandlingService.tilbakestillBehandlingTilVilkårsvurdering(aktivOgÅpenBehandling)
                "Tilbakestiller behandling $aktivOgÅpenBehandling til vilkårsvurderingen"
            } else {
                "Behandling $aktivOgÅpenBehandling er under utredning, men er allerede i riktig tilstand."
            }

            logger.info(brukerHarÅpenBehandlingMelding)
            return brukerHarÅpenBehandlingMelding
        }

        val behandlingEtterBehandlingsresultat =
            autovedtakService.opprettAutomatiskBehandlingOgKjørTilBehandlingsresultat(
                aktør = søkerAktør,
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

        return "Satsendring kjørt OK"
    }

    /**
     * Eksempel på hvordan man kan opprette tasker for behandling av fagsaker som
     * har gammel sats.
     *
     * Annoter metoden med @Scheduled(cron = "0 0 7 * * *") så vil den prøve å plukke fagsaker
     * hver morgen. Ersatt kjøringsmåned med måned for ny sats, eller måneden man ønsker å kjøre satsendring
     *
     * Vi forsøker flere dager for å plukke opp eventuelle behandlinger
     * som blir iverksatt med gammel sats som har ligget åpne.
     */
    fun scheduledFinnOgOpprettTaskerForSatsendring() {
        if (LeaderClient.isLeader() == false) return

        val kjøringsmåned = YearMonth.of(2022, 1)

        // Vi ønsker kun å opprette tasker i inneværende satsendringsmåned som nå er januar 2022
        if (YearMonth.now() != kjøringsmåned) {
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
     * Siden den siste iverksatte ikke nødvendigvis er den aktive/åpne kan det være
     * åpne behandlinger på fagsaken det kjøres satsendring for. Dette blir håndtert i kjøringen
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

    private fun harAlleredeNySats(behandlingId: Long): Boolean {
        val andeler =
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = behandlingId)

        return andeler.any {
            it.sats == sisteTilleggOrdinærSats.beløp
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(AutovedtakSatsendringService::class.java)
        val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}
