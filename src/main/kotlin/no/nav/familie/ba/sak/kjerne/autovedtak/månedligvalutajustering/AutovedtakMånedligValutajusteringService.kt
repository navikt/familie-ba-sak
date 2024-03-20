package no.nav.familie.ba.sak.kjerne.autovedtak.månedligvalutajustering

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.LocalDateProvider
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.FerdigstillBehandlingTask
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.ba.sak.task.MånedligValutajusteringFinnFagsakerTask
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

@Service
class AutovedtakMånedligValutajusteringService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val autovedtakService: AutovedtakService,
    private val taskRepository: TaskRepositoryWrapper,
    private val behandlingService: BehandlingService,
    private val localDateProvider: LocalDateProvider,
    private val kompetanseService: KompetanseService,
) {
    val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun utførMånedligValutajustering(
        behandlingid: Long,
        måned: YearMonth,
    ) {
        logger.info("Utfører månedlig valutajustering for behandling=$behandlingid og måned=$måned")

        if (måned != localDateProvider.now().toYearMonth()) {
            throw Feil("Prøver å utføre månedlig valutajustering for en annen måned enn nåværende måned.")
        }

        val behandling = behandlingHentOgPersisterService.hent(behandlingid)
        val sisteVedtatteBehandling = behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id) ?: error("Fant ikke siste vedtatte behandling for ${behandling.fagsak.id}")
        val aktivOgÅpenBehandling =
            behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(
                fagsakId = sisteVedtatteBehandling.fagsak.id,
            )

        validerAtViKanUtføreMånedligValutajustering(behandling, sisteVedtatteBehandling)

        // hvis siste vedtatte behandling er valutajustering og opprettet tidspunkt er denne måned hopp ut
        if (behandling.opprettetTidspunkt.toLocalDate().toYearMonth() == måned &&
            behandling.opprettetÅrsak == BehandlingÅrsak.MÅNEDLIG_VALUTAJUSTERING
        ) {
            logger.info("Valutajustering er allerede gjort denne måneden for fagsak ${behandling.fagsak.id}. Hopper ut av valutajusteringen.")
            return
        }

        if (aktivOgÅpenBehandling != null) {
            // todo: legg til støtte for å sette på maskinell vent/snike i køen
            throw Feil("Ikke lagt til støtte for å sette på maskinell vent")
        }

        val søkerAktør = sisteVedtatteBehandling.fagsak.aktør

        val behandlingEtterBehandlingsresultat =
            autovedtakService.opprettAutomatiskBehandlingOgKjørTilBehandlingsresultat(
                aktør = søkerAktør,
                behandlingType = BehandlingType.REVURDERING,
                behandlingÅrsak = BehandlingÅrsak.MÅNEDLIG_VALUTAJUSTERING,
                fagsakId = sisteVedtatteBehandling.fagsak.id,
            )

        val opprettetVedtak =
            autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(
                behandlingEtterBehandlingsresultat,
            )

        val task =
            when (behandlingEtterBehandlingsresultat.steg) {
                StegType.IVERKSETT_MOT_OPPDRAG -> {
                    IverksettMotOppdragTask.opprettTask(
                        behandlingEtterBehandlingsresultat,
                        opprettetVedtak,
                        SikkerhetContext.hentSaksbehandler(),
                    )
                }

                StegType.FERDIGSTILLE_BEHANDLING -> {
                    behandlingService.oppdaterStatusPåBehandling(
                        behandlingEtterBehandlingsresultat.id,
                        BehandlingStatus.IVERKSETTER_VEDTAK,
                    )
                    FerdigstillBehandlingTask.opprettTask(
                        søkerAktør.aktivFødselsnummer(),
                        behandlingEtterBehandlingsresultat.id,
                    )
                }

                else -> throw Feil("Ugyldig neste steg ${behandlingEtterBehandlingsresultat.steg} ved månedlig valutajustering for fagsak=${behandling.fagsak.id}")
            }

        taskRepository.save(task)
    }

    @Transactional
    fun utførMånedligValutajusteringPåFagsak(
        fagsakId: Long,
        måned: YearMonth,
    ) {
        logger.info("Utfører månedlig valutajustering for fagsak=$fagsakId og måned=$måned")
        val sisteVedtatteBehandling = behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId = fagsakId)
        if (sisteVedtatteBehandling == null) {
            logger.info("Fant ingen vedtatte behandlinger for fagsak=$fagsakId. Hopper ut av valutajusteringen.")
            return
        }

        val kompetanserPåSisteVedtatteBehandling = kompetanseService.hentKompetanser(BehandlingId(sisteVedtatteBehandling.id))
        val erSekundærland =
            MånedligValutajusteringFinnFagsakerTask.erSekundærlandIMåned(kompetanserPåSisteVedtatteBehandling, måned)

        if (erSekundærland) {
            utførMånedligValutajustering(behandlingid = sisteVedtatteBehandling.id, måned)
        } else {
            logger.info("Site behandling er ikke sekundærland for måned=$måned")
        }
    }

    companion object {
        fun validerAtViKanUtføreMånedligValutajustering(
            behandling: Behandling,
            sisteVedtatteBehandling: Behandling,
        ) {
            if (behandling.fagsak.status != FagsakStatus.LØPENDE) throw Feil("Forsøker å utføre satsendring på ikke løpende fagsak ${behandling.fagsak.id}")

            if (sisteVedtatteBehandling != behandling) {
                throw Feil("Siste vedtatte behandling er ikke lik behandling som vi har hentet kompetanse fra.")
            }
        }
    }
}
