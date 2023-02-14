package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.HenleggÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.RestHenleggBehandlingInfo
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = HenleggBehandlingTask.TASK_STEP_TYPE,
    beskrivelse = "Henlegg behandling",
    maxAntallFeil = 1
)
class HenleggBehandlingTask(
    val arbeidsfordelingService: ArbeidsfordelingService,
    val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    val stegService: StegService,
    val oppgaveService: OppgaveService
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val henleggBehandlingTaskDTO = objectMapper.readValue(task.payload, HenleggBehandlingTaskDTO::class.java)
        val behandling = behandlingHentOgPersisterService.hent(henleggBehandlingTaskDTO.behandlingId)

        if (behandling.status == BehandlingStatus.AVSLUTTET) {
            task.metadata["Resultat"] = "Behandlingen er allerede avsluttet"
            return
        }

        if (henleggBehandlingTaskDTO.validerOppgavefristErEtterDato != null) {
            val valideringsdato = henleggBehandlingTaskDTO.validerOppgavefristErEtterDato
            val frist = oppgaveService.hentOppgaverSomIkkeErFerdigstilt(Oppgavetype.BehandleSak, behandling).let {
                it.singleOrNull()?.run {
                    oppgaveService.hentOppgave(gsakId.toLong()).fristFerdigstillelse ?: error("Oppgave $gsakId mangler frist")
                } ?: error("Behandling ${behandling.id} har ingen, eller mer enn en behandleSak-oppgave: $it")
            }
            if (!LocalDate.parse(frist).isAfter(henleggBehandlingTaskDTO.validerOppgavefristErEtterDato)) {
                task.metadata["Resultat"] = "Stoppet. Behandlingen har frist $frist. Må være etter $valideringsdato"
                return
            }
        }

        stegService.håndterHenleggBehandling(
            behandling = behandling,
            henleggBehandlingInfo = henleggBehandlingTaskDTO.run { RestHenleggBehandlingInfo(årsak, begrunnelse) }
        ).apply {
            task.metadata["behandlendeEnhetId"] = arbeidsfordelingService.hentArbeidsfordelingPåBehandling(id).behandlendeEnhetId
            task.metadata["fagsakId"] = fagsak.id
            task.metadata["Resultat"] = "Henleggelse kjørt OK"
        }
    }

    companion object {

        const val TASK_STEP_TYPE = "HenleggBehandling"

        fun opprettTask(henleggBehandlingTaskDTO: HenleggBehandlingTaskDTO): Task {
            return Task(
                type = TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(henleggBehandlingTaskDTO),
                properties = Properties().apply {
                    this["behandlingId"] = henleggBehandlingTaskDTO.behandlingId.toString()
                }
            )
        }
    }
}

class HenleggBehandlingTaskDTO(
    val behandlingId: Long,
    val årsak: HenleggÅrsak,
    val begrunnelse: String,
    val validerOppgavefristErEtterDato: LocalDate?
)
