package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.integrasjoner.økonomi.OffsetOppdatering
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = RettOffsetIAndelTilkjentYtelseTask.TASK_STEP_TYPE,
    beskrivelse = "Rett feilaktige offsets i andel tilkjent ytelse",
    maxAntallFeil = 1
)
class RettOffsetIAndelTilkjentYtelseTask(
    val behandlingRepository: BehandlingRepository,
    val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    val beregningService: BeregningService,
    val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val payload =
            objectMapper.readValue(task.payload, RettOffsetIAndelTilkjentYtelseDto::class.java)
        val behandlingerMedFeilaktigeOffsets =
            behandlingRepository.finnBehandlingerMedDuplikateOffsetsForAndelTilkjentYtelse()
                .map { behandlingRepository.finnBehandling(it) }

        val relevanteBehandlinger = behandlingerMedFeilaktigeOffsets
            .map { it.fagsak }
            .map { behandlingRepository.finnBehandlinger(it.id).sortedBy { behandling -> behandling.id } }
            .map { it.last() }
            .filter {
                if (payload.kunSiste) behandlingerMedFeilaktigeOffsets.contains(it)
                else !behandlingerMedFeilaktigeOffsets.contains(it)
            }

        relevanteBehandlinger
            .forEach {
                val oppdatertTilstand =
                    beregningService.hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(behandlingId = it.id)

                if (oppdatertTilstand.isNotEmpty()) {
                    val forrigeBehandling =
                        behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(behandling = it)
                            ?: error("Finner ikke forrige behandling ved oppdatering av tilkjent ytelse og iverksetting av vedtak")

                    val beståendeAndelerMedOppdatertOffset = ØkonomiUtils.finnBeståendeAndelerMedOppdatertOffset(
                        oppdaterteKjeder = ØkonomiUtils.kjedeinndelteAndeler(oppdatertTilstand),
                        forrigeKjeder = beregningService.hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(
                            forrigeBehandling.id
                        ).let { ØkonomiUtils.kjedeinndelteAndeler(it) }
                    )

                    val logglinjer = beståendeAndelerMedOppdatertOffset.map { oppdatering -> formaterLogglinje(oppdatering) }
                        .joinToString(separator = System.lineSeparator())
                    secureLogger.info("Behandling: $it, logglinjer: $logglinjer")

                    if (!payload.simuler) {
                        beståendeAndelerMedOppdatertOffset.forEach { oppdatering -> oppdatering.oppdater() }
                    }
                }
            }

        if (payload.kunSiste) {
            val diff = behandlingerMedFeilaktigeOffsets.minus(relevanteBehandlinger.toSet())
            if (diff.isNotEmpty()) secureLogger.warn("Behandlinger med feilaktige offsets, der fagsaka har fått ei nyere behandling: $diff")
        }
    }

    private fun formaterLogglinje(oppdatering: OffsetOppdatering) =
        "Oppdaterer andel tilkjent ytelse ${oppdatering.beståendeIOppdatert} med periodeoffset=${oppdatering.periodeOffset}, forrigePeriodeOffset=${oppdatering.forrigePeriodeOffset} og kildeBehandlingId=${oppdatering.kildeBehandlingId}"

    companion object {
        const val TASK_STEP_TYPE = "rettOffsetIAndelTilkjentYtelseTask"
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}

data class RettOffsetIAndelTilkjentYtelseDto(
    val simuler: Boolean,
    val kunSiste: Boolean
)
