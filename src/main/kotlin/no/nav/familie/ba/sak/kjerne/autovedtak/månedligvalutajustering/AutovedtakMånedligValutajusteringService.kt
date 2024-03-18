package no.nav.familie.ba.sak.kjerne.autovedtak.månedligvalutajustering

import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.MånedligValutaJusteringFeil
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.SettPåMaskinellVentÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.SnikeIKøenService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.FerdigstillBehandlingTask
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
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
) {
    private val månedligvalutajusteringIgnorertÅpenBehandling = Metrics.counter("valutajustering.ignorert.aapenbehandling")

    @Transactional
    fun utførMånedligValutajustering(
        behandlingid: Long,
        måned: YearMonth,
    ) {
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
            return
        }

        if (aktivOgÅpenBehandling != null) {
            if (snikeIKøenService.kanSnikeForbi(aktivOgÅpenBehandling)) {
                snikeIKøenService.settAktivBehandlingTilPåMaskinellVent(
                    aktivOgÅpenBehandling.id,
                    SettPåMaskinellVentÅrsak.SATSENDRING,
                )
            } else {
                månedligvalutajusteringIgnorertÅpenBehandling.increment()
                throw MånedligValutaJusteringFeil(melding = "Kan ikke utføre månedlig valutajustering for fagsak=${behandling.fagsak.id} fordi det er en åpen behandling vi ikke klarer å snike forbi")
            }
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
