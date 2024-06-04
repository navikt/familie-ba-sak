package no.nav.familie.ba.sak.kjerne.autovedtak.månedligvalutajustering

import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.LocalDateProvider
import no.nav.familie.ba.sak.common.MånedligValutaJusteringFeil
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.SettPåMaskinellVentÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.SnikeIKøenService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursService
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.erAlleValutakurserOppdaterteIMåned
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.FerdigstillBehandlingTask
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

@Service
class AutovedtakMånedligValutajusteringService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val autovedtakService: AutovedtakService,
    private val snikeIKøenService: SnikeIKøenService,
    private val taskRepository: TaskRepositoryWrapper,
    private val behandlingService: BehandlingService,
    private val localDateProvider: LocalDateProvider,
    private val valutakursService: ValutakursService,
) {
    private val månedligvalutajusteringIgnorertÅpenBehandling = Metrics.counter("valutajustering.ignorert.aapenbehandling")

    val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun utførMånedligValutajustering(
        fagsakId: Long,
        måned: YearMonth,
    ) {
        logger.info("Utfører månedlig valutajustering for fagsak=$fagsakId og måned=$måned")

        if (måned != localDateProvider.now().toYearMonth()) {
            throw Feil("Prøver å utføre månedlig valutajustering for en annen måned enn nåværende måned.")
        }

        val sisteVedtatteBehandling = behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId = fagsakId) ?: error("Fant ikke siste vedtatte behandling for $fagsakId")
        val sisteValutakurser = valutakursService.hentValutakurser(BehandlingId(sisteVedtatteBehandling.id))
        if (sisteValutakurser.erAlleValutakurserOppdaterteIMåned(måned)) {
            logger.info("Valutakursene er allerede oppdatert for fagsak $fagsakId. Hopper ut")
            return
        }

        if (sisteVedtatteBehandling.fagsak.status != FagsakStatus.LØPENDE) {
            throw Feil("Forsøker å utføre satsendring på ikke løpende fagsak $fagsakId")
        }

        val aktivOgÅpenBehandling = behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsakId = fagsakId)

        if (aktivOgÅpenBehandling != null) {
            if (snikeIKøenService.kanSnikeForbi(aktivOgÅpenBehandling)) {
                snikeIKøenService.settAktivBehandlingPåMaskinellVent(
                    aktivOgÅpenBehandling.id,
                    SettPåMaskinellVentÅrsak.MÅNEDLIG_VALUTAJUSTERING,
                )
            } else {
                månedligvalutajusteringIgnorertÅpenBehandling.increment()
                throw MånedligValutaJusteringFeil(melding = "Kan ikke utføre månedlig valutajustering for fagsak=$fagsakId fordi det er en åpen behandling vi ikke klarer å snike forbi")
            }
        }

        val søkerAktør = sisteVedtatteBehandling.fagsak.aktør

        val behandlingEtterBehandlingsresultat =
            autovedtakService.opprettAutomatiskBehandlingOgKjørTilBehandlingsresultat(
                aktør = søkerAktør,
                behandlingType = BehandlingType.REVURDERING,
                behandlingÅrsak = BehandlingÅrsak.MÅNEDLIG_VALUTAJUSTERING,
                fagsakId = fagsakId,
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

                else -> throw Feil("Ugyldig neste steg ${behandlingEtterBehandlingsresultat.steg} ved månedlig valutajustering for fagsak=$fagsakId")
            }
        taskRepository.save(task)
    }
}
