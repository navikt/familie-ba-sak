package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.task.OpprettBehandleSakOppgaveForNyBehandlingTask
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service

@Service
class RegistrerPersongrunnlag(
        private val persongrunnlagService: PersongrunnlagService,
        private val featureToggleService: FeatureToggleService,
        private val taskRepository: TaskRepository
) : BehandlingSteg<Registreringsdata> {

    override fun utførSteg(behandling: Behandling, data: Registreringsdata): Behandling {
        persongrunnlagService.lagreSøkerOgBarnIPersonopplysningsgrunnlaget(data.ident, data.barnasIdenter, behandling)
        if (featureToggleService.isEnabled("familie-ba-sak.lag-oppgave")) {
            val nyTask = Task.nyTask(OpprettBehandleSakOppgaveForNyBehandlingTask.TASK_STEP_TYPE, behandling.id.toString())
            taskRepository.save(nyTask)
        } else {
            BehandlingService.LOG.info("Lag opprettOppgaveTask er skrudd av i miljø")
        }

        return behandling
    }

    override fun nesteSteg(behandling: Behandling): StegType {
        return StegType.VILKÅRSVURDERING
    }

    override fun stegType(): StegType {
        return StegType.REGISTRERE_PERSONGRUNNLAG
    }
}

data class Registreringsdata(
        val ident: String,
        val barnasIdenter: List<String>)