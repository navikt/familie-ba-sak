package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.vilkår.VilkårService
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.task.OpprettBehandleSakOppgaveForNyBehandlingTask
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RegistrerPersongrunnlag(
        private val persongrunnlagService: PersongrunnlagService,
        private val featureToggleService: FeatureToggleService,
        private val taskRepository: TaskRepository,
        private val vilkårService: VilkårService
) : BehandlingSteg<RegistrerPersongrunnlagDTO> {

    @Transactional
    override fun utførStegOgAngiNeste(behandling: Behandling, data: RegistrerPersongrunnlagDTO): StegType {
        persongrunnlagService.lagreSøkerOgBarnIPersonopplysningsgrunnlaget(data.ident, data.barnasIdenter, behandling)
        vilkårService.initierVilkårvurderingForBehandling(behandling.id)
        if (featureToggleService.isEnabled("familie-ba-sak.lag-oppgave")) {
            val nyTask = Task.nyTask(OpprettBehandleSakOppgaveForNyBehandlingTask.TASK_STEP_TYPE, behandling.id.toString())
            taskRepository.save(nyTask)
        } else {
            BehandlingService.LOG.info("Lag opprettOppgaveTask er skrudd av i miljø")
        }

        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType {
        return StegType.REGISTRERE_PERSONGRUNNLAG
    }
}

data class RegistrerPersongrunnlagDTO(
        val ident: String,
        val barnasIdenter: List<String>)