package no.nav.familie.ba.sak.kjerne.autovedtak.månedligvalutajustering

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.FerdigstillBehandlingTask
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.ba.sak.task.MånedligValutajusteringTaskDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AutovedtakMånedligValutajusteringService(private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService, private val autovedtakService: AutovedtakService, private val taskRepository: TaskRepositoryWrapper, private val behandlingService: BehandlingService) {
    @Transactional
    fun utførMånedligValutajustering(månedligValutajusteringTaskDto: MånedligValutajusteringTaskDto) {
        val behandling = behandlingHentOgPersisterService.hent(månedligValutajusteringTaskDto.behandlingid)
        val sisteVedtatteBehandling = behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id) ?: error("Fant ikke siste vedtatte behandling for ${behandling.fagsak.id}")
        if (behandling.fagsak.status != FagsakStatus.LØPENDE) throw Feil("Forsøker å utføre satsendring på ikke løpende fagsak ${behandling.fagsak.id}")

        if (sisteVedtatteBehandling != behandling) {
            throw Feil("Siste vedtatte behandling er ikke lik behandling som vi har hentet kompetanse fra.")
        }

        // hvis siste vedtatte behandling er valutajustering og opprettet tidspunkt er denne måned hopp ut
        if (behandling.opprettetTidspunkt.toLocalDate().toYearMonth() == månedligValutajusteringTaskDto.måned && behandling.type == BehandlingType.REVURDERING && behandling.opprettetÅrsak == BehandlingÅrsak.MÅNEDLIG_VALUTAJUSTERING) {
            return
        }

        val aktivOgÅpenBehandling =
            behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsakId = sisteVedtatteBehandling.fagsak.id)
        val søkerAktør = sisteVedtatteBehandling.fagsak.aktør

        if (aktivOgÅpenBehandling != null) {
            // todo: legg til støtte for å sette på maskinell vent
            throw Feil("Ikke lagt til støtte for å sette på maskinell vent")
        }

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
}
