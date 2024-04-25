package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.integrasjoner.økonomi.utledStønadTom
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = OppdaterStønadTomPåTilkjentYtelseTask.TASK_STEP_TYPE,
    beskrivelse = "Oppdaterer tilkjentYtelse.stønadTom for siste iverksatte behandling på gitt fagsak.",
    maxAntallFeil = 1,
)
class OppdaterStønadTomPåTilkjentYtelseTask(
    private val behandlingRepository: BehandlingRepository,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val endretUtbetalingAndelRepository: EndretUtbetalingAndelRepository,
) : AsyncTaskStep {
    private val logger = LoggerFactory.getLogger(OppdaterStønadTomPåTilkjentYtelseTask::class.java)

    override fun doTask(task: Task) {
        val fagsakId = objectMapper.readValue(task.payload, Long::class.java)
        logger.info("Oppdaterer stønadTom for TilkjentYtelse på fagsak $fagsakId")

        val behandling =
            behandlingRepository.finnBehandlinger(fagsakId)
                .filter { it.status == BehandlingStatus.AVSLUTTET && !it.erHenlagt() }
                .sortedByDescending { it.aktivertTidspunkt }
                .firstOrNull() ?: throw Feil("Finner ingen behandling på fagsak $fagsakId")

        if (behandling.fagsak.status != FagsakStatus.LØPENDE) {
            throw Feil("Prøvde å oppdatere stønadTom på en tilkjentYtelse på en fagsak som ikke er løpende. ")
        }

        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandling(behandling.id)
        val endredeUtbetalinger = endretUtbetalingAndelRepository.findByBehandlingId(behandling.id)

        val stønadTom = utledStønadTom(tilkjentYtelse.andelerTilkjentYtelse, endredeUtbetalinger)
        val gammelStønadTom = tilkjentYtelse.stønadTom

        if (stønadTom == gammelStønadTom) {
            logger.warn("Ingen endring i stønadTom for fagsak/behandling: $fagsakId/${behandling.id}")
        }

        tilkjentYtelse.stønadTom = stønadTom

        tilkjentYtelseRepository.save(tilkjentYtelse)
        logger.info("Ferdig med å oppdatere stønadTom fra $gammelStønadTom til $stønadTom for TilkjentYtelse på fagsak/behandling: $fagsakId/${behandling.id}")
    }

    companion object {
        fun opprettStønadTomTask(fagsakId: Long): Task {
            return Task(
                type = TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(fagsakId),
            )
        }

        const val TASK_STEP_TYPE = "oppdaterStonadTom"
    }
}
