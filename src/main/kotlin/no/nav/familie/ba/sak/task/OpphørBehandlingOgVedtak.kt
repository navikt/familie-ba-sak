package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.vedtak.Vedtak
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
@TaskStepBeskrivelse(taskStepType = IverksettMotOppdrag.TASK_STEP_TYPE, beskrivelse = "Opphør aktiv behandling og vedtak", maxAntallFeil = 3)
class OpphørBehandlingOgVedtak(
        private val behandlingService: BehandlingService,
        private val taskRepository: TaskRepository
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val opphørVedtakTask = objectMapper.readValue(task.payload, OpphørVedtakDTO::class.java)

        IverksettMotOppdrag.LOG.debug("Opphører behandling og tilhørende vedtak med behandlingsId ${opphørVedtakTask.gjeldendeBehandlingsId}")
        behandlingService.opphørVedtak(opphørVedtakTask.saksbehandlerId,
                                       opphørVedtakTask.gjeldendeBehandlingsId,
                                       BehandlingType.valueOf(opphørVedtakTask.nyBehandlingType),
                                       ::opprettIverksettMotOppdragTask)
    }

    fun opprettIverksettMotOppdragTask(vedtak : Vedtak)  {
        val nyTask = IverksettMotOppdrag.opprettTask(
                vedtak.behandling.fagsak.personIdent.ident,
                vedtak.behandling.id!!,
                vedtak.id!!,
                vedtak.ansvarligSaksbehandler)

        taskRepository.save(nyTask)
    }

    companion object {
        const val TASK_STEP_TYPE = "opphørVedtak"
        val LOG = LoggerFactory.getLogger(IverksettMotOppdrag::class.java)

        fun opprettTaskOpphørBehandlingOgVedtak(gjeldendeBehandling: Behandling,
                                                 gjeldendeVedtak: Vedtak,
                                                 saksbehandlerId: String,
                                                 nyBehandlingstype: BehandlingType) : Task {

            return Task.nyTask(type = TASK_STEP_TYPE,
                               payload = objectMapper.writeValueAsString(OpphørVedtakDTO(
                                       personIdent = gjeldendeBehandling.fagsak.personIdent.ident,
                                       gjeldendeBehandlingsId = gjeldendeBehandling.id!!,
                                       gjeldendeVedtaksId = gjeldendeVedtak.id!!,
                                       saksbehandlerId = saksbehandlerId,
                                       nyBehandlingType = nyBehandlingstype.name
                               )),
                               properties = Properties().apply {
                                   this["personIdent"] = gjeldendeBehandling.fagsak.personIdent.ident
                                   this["behandlingsId"] = gjeldendeBehandling.id.toString()
                               }
            )
        }
    }

}

data class OpphørVedtakDTO(
        val personIdent: String,
        val gjeldendeBehandlingsId : Long,
        val gjeldendeVedtaksId: Long,
        val saksbehandlerId : String,
        val nyBehandlingType : String
)