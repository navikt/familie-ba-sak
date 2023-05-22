package no.nav.familie.ba.sak.kjerne.behandling

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.error.RekjørSenereException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = ReaktiverÅpenBehandlingTask.TASK_STEP_TYPE,
    beskrivelse = "Reaktiverer deaktivert behandling",
    maxAntallFeil = 1,
)
class ReaktiverÅpenBehandlingTask(
    private val behandlingRepository: BehandlingRepository,
) : AsyncTaskStep {

    private val logger = LoggerFactory.getLogger(javaClass)
    override fun doTask(task: Task) {
        val dto = objectMapper.readValue<ReaktiverÅpenBehandlingTaskDto>(task.payload)
        val behandlingerForFagsak =
            behandlingRepository.finnBehandlinger(dto.fagsakId).sortedByDescending { it.opprettetTidspunkt }
        val sisteBehandling = behandlingerForFagsak[0]
        val åpenBehandling = behandlingerForFagsak[1]

        validerBehandlinger(sisteBehandling, åpenBehandling, dto)

        if (sisteBehandling.status != BehandlingStatus.AVSLUTTET) {
            throw RekjørSenereException("Behandling er ikke avsluttet ennå", LocalDateTime.now().plusMinutes(5))
        }

        logger.info("Deaktiverer ${sisteBehandling.id} og aktiverer ${åpenBehandling.id}")
        sisteBehandling.aktiv = false
        åpenBehandling.aktiv = true
        åpenBehandling.aktivertTidspunkt = LocalDateTime.now()
        åpenBehandling.status = BehandlingStatus.UTREDES
        // TODO tilbakestill vedtak/brev etc på åpen behandling ?
        behandlingRepository.save(sisteBehandling)
        behandlingRepository.save(åpenBehandling)
    }

    private fun validerBehandlinger(
        sisteBehandling: Behandling,
        åpenBehandling: Behandling,
        dto: ReaktiverÅpenBehandlingTaskDto,
    ) {
        if (sisteBehandling.id != dto.behandlingSomSniketIKøen) {
            error("Siste behandling=${sisteBehandling.id} for fagsak er ikke behandlingen som sniket i køen(${dto.behandlingSomSniketIKøen})")
        }
        if (åpenBehandling.id != dto.åpenBehandlingId) {
            error("Nest siste behandling=${åpenBehandling.id} for fagsak er ikke behandlingen som er satt på vent(${dto.åpenBehandlingId})")
        }
        validerStatePåBehandlinger(åpenBehandling, sisteBehandling)
    }

    companion object {
        const val TASK_STEP_TYPE = "reaktiverÅpenBehandlingEtterSnikingIKøen"

        fun opprettTask(åpenBehandling: Behandling, behandlingSomSniketIKøen: Behandling): Task {
            validerStatePåBehandlinger(åpenBehandling, behandlingSomSniketIKøen)

            val fagsakId = åpenBehandling.fagsak.id
            val properties = Properties().apply {
                setProperty("fagsakId", fagsakId.toString())
                setProperty("åpenBehandlingId", åpenBehandling.id.toString())
                setProperty("behandlingSomSniketIKøen", behandlingSomSniketIKøen.id.toString())
            }
            val payload = ReaktiverÅpenBehandlingTaskDto(fagsakId, åpenBehandling.id, behandlingSomSniketIKøen.id)
            return Task(TASK_STEP_TYPE, objectMapper.writeValueAsString(payload), properties)
        }
    }
}

private fun validerStatePåBehandlinger(
    åpenBehandling: Behandling,
    behandlingSomSniketIKøen: Behandling,
) {
    if (åpenBehandling.fagsak != behandlingSomSniketIKøen.fagsak) {
        error("Behandlinger er koblet til ulike fagsaker. åpenBehandling=$åpenBehandling behandlingSomSniketIKøen=$behandlingSomSniketIKøen")
    }
    if (åpenBehandling.aktiv || åpenBehandling.status != BehandlingStatus.SATT_PÅ_VENT) {
        error("Åpen behandling har feil state $åpenBehandling")
    }
    if (!behandlingSomSniketIKøen.aktiv) {
        error("Behandling som sniket i køen må være aktiv $behandlingSomSniketIKøen")
    }
    if (behandlingSomSniketIKøen.status == BehandlingStatus.SATT_PÅ_VENT) {
        error("Behandling=${behandlingSomSniketIKøen.id} som sniket i køen kan ikke ha status satt på vent")
    }
}

private data class ReaktiverÅpenBehandlingTaskDto(
    val fagsakId: Long,
    val åpenBehandlingId: Long,
    val behandlingSomSniketIKøen: Long,
)
