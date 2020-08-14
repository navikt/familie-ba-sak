package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.task.OpprettOppgaveTask
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class SendTilManuellBehandling(
        private val taskRepository: TaskRepository
        private val featureToggleService: FeatureToggleService
) {

    fun opprettOppgave(behandlingId: Long) {
        if (featureToggleService.isEnabled("familie-ba-sak.lag-oppgave")
            && !featureToggleService.isEnabled("familie-ba-sak.rollback-automatisk-regelkjoring")) {

            val nyTask = OpprettOppgaveTask.opprettTask(
                    behandlingId = behandlingId,
                    oppgavetype = Oppgavetype.BehandleSak,
                    fristForFerdigstillelse = LocalDate.now()
            )
            taskRepository.save(nyTask)
        } else {
            StegService.LOG.info("Lag opprettOppgaveTask er skrudd av i miljø eller behandlingen av fødselshendelsen var innvilget")
        }
    }
}