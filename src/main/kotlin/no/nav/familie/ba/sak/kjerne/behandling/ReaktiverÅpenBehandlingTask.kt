package no.nav.familie.ba.sak.kjerne.behandling

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.error.RekjørSenereException
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
    private val snikeIKøenService: SnikeIKøenService,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val dto = objectMapper.readValue<ReaktiverÅpenBehandlingTaskDto>(task.payload)
        try {
            snikeIKøenService.reaktiverBehandlingPåMaskinellVent(
                dto.fagsakId,
                dto.åpenBehandlingId,
                dto.behandlingSomSniketIKøen,
            )
        } catch (e: BehandlingErIkkeAvsluttetException) {
            throw RekjørSenereException(
                "Behandling=${dto.behandlingSomSniketIKøen} er ikke avsluttet ennå",
                LocalDateTime.now().plusMinutes(5),
            )
        }
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
    if (åpenBehandling.opprettetTidspunkt > behandlingSomSniketIKøen.opprettetTidspunkt) {
        error("Åpen behandling er opprettet etter behandling som sniket i køen")
    }
}

private data class ReaktiverÅpenBehandlingTaskDto(
    val fagsakId: Long,
    val åpenBehandlingId: Long,
    val behandlingSomSniketIKøen: Long,
)
