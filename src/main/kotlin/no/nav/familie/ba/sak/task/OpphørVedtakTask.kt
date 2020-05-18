package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*

@Service
@TaskStepBeskrivelse(taskStepType = OpphørVedtakTask.TASK_STEP_TYPE,
                     beskrivelse = "Opphør aktiv behandling og vedtak",
                     maxAntallFeil = 3)
class OpphørVedtakTask(
        private val vedtakService: VedtakService,
        private val taskRepository: TaskRepository
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val opphørVedtakTask = objectMapper.readValue(task.payload, OpphørVedtakTaskDTO::class.java)

        LOG.debug("Opphører behandling og tilhørende vedtak med behandlingsId ${opphørVedtakTask.gjeldendeBehandlingsId}")
        vedtakService.opphørVedtak(opphørVedtakTask.saksbehandlerId,
                                   opphørVedtakTask.gjeldendeBehandlingsId,
                                   BehandlingType.valueOf(opphørVedtakTask.nyBehandlingType),
                                   opphørVedtakTask.opphørsdato,
                                   ::opprettIverksettMotOppdragTask)
    }

    fun opprettIverksettMotOppdragTask(vedtak: Vedtak) {
        val nyTask = IverksettMotOppdragTask.opprettTask(
                vedtak.behandling.fagsak.hentAktivIdent().ident,
                vedtak.behandling.id,
                vedtak.id,
                vedtak.ansvarligSaksbehandler)

        taskRepository.save(nyTask)
    }

    companion object {
        const val TASK_STEP_TYPE = "opphørVedtak"
        val LOG = LoggerFactory.getLogger(OpphørVedtakTaskDTO::class.java)

        fun opprettOpphørVedtakTask(gjeldendeBehandling: Behandling,
                                    gjeldendeVedtak: Vedtak,
                                    saksbehandlerId: String,
                                    nyBehandlingstype: BehandlingType,
                                    opphørsdato: LocalDate): Task {

            return Task.nyTask(type = TASK_STEP_TYPE,
                               payload = objectMapper.writeValueAsString(OpphørVedtakTaskDTO(
                                       personIdent = gjeldendeBehandling.fagsak.hentAktivIdent().ident,
                                       gjeldendeBehandlingsId = gjeldendeBehandling.id,
                                       gjeldendeVedtaksId = gjeldendeVedtak.id,
                                       saksbehandlerId = saksbehandlerId,
                                       nyBehandlingType = nyBehandlingstype.name,
                                       opphørsdato = opphørsdato
                               )),
                               properties = Properties().apply {
                                   this["personIdent"] = gjeldendeBehandling.fagsak.hentAktivIdent().ident
                                   this["behandlingsId"] = gjeldendeBehandling.id.toString()
                               }
            )
        }
    }

}

data class OpphørVedtakTaskDTO(
        val personIdent: String,
        val gjeldendeBehandlingsId: Long,
        val gjeldendeVedtaksId: Long,
        val saksbehandlerId: String,
        val nyBehandlingType: String,
        val opphørsdato: LocalDate
)