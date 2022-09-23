package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.integrasjoner.økonomi.OffsetOppdatering
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
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
    val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    val beregningService: BeregningService
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val payload =
            objectMapper.readValue(task.payload, RettOffsetIAndelTilkjentYtelseDto::class.java)
        køyrTask(payload)
    }

    private fun køyrTask(payload: RettOffsetIAndelTilkjentYtelseDto) {
        val behandlingerMedFeilaktigeOffsets = payload.behandlinger.map { behandlingHentOgPersisterService.hent(it) }

        if (behandlingerMedFeilaktigeOffsets.isNotEmpty()) {
            secureLogger.warn(
                "Behandlinger med feilaktige offsets: ${
                behandlingerMedFeilaktigeOffsets.map { it.id }.joinToString(separator = ",")
                }"
            )
        }

        val relevanteBehandlinger = behandlingerMedFeilaktigeOffsets
            .map { it.fagsak }
            .map { behandlingHentOgPersisterService.hentBehandlinger(it.id).sortedBy { behandling -> behandling.id } }
            .map { it.last() }
            .filter {
                behandlingerMedFeilaktigeOffsets.contains(it) || arrayOf(
                    BehandlingStatus.OPPRETTET,
                    BehandlingStatus.UTREDES,
                    BehandlingStatus.FATTER_VEDTAK
                ).any { status -> status == it.status }
            }

        secureLogger.warn(
            "Behandlinger med duplisert offset:\n ${
            relevanteBehandlinger.map { it.id }.joinToString(",")
            }"
        )

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

                    val logglinjer =
                        beståendeAndelerMedOppdatertOffset.joinToString(separator = System.lineSeparator()) { oppdatering ->
                            formaterLogglinje(
                                oppdatering
                            )
                        }
                    secureLogger.info("Behandling: $it, logglinjer: $logglinjer")

                    if (!payload.simuler) {
                        beståendeAndelerMedOppdatertOffset.forEach { oppdatering -> oppdatering.oppdater() }
                    }
                }
            }

        val diff = behandlingerMedFeilaktigeOffsets.minus(relevanteBehandlinger.toSet())
        if (diff.isNotEmpty()) secureLogger.warn("Behandlinger med feilaktige offsets, der fagsaka har fått ei nyere behandling som er avslutta: $diff")
    }

    private fun formaterLogglinje(oppdatering: OffsetOppdatering) =
        "Oppdaterer andel tilkjent ytelse ${oppdatering.beståendeAndelSomSkalHaOppdatertOffset} med periodeoffset=${oppdatering.periodeOffset}, forrigePeriodeOffset=${oppdatering.forrigePeriodeOffset} og kildeBehandlingId=${oppdatering.kildeBehandlingId}"

    companion object {
        const val TASK_STEP_TYPE = "rettOffsetIAndelTilkjentYtelseTask"
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}

data class RettOffsetIAndelTilkjentYtelseDto(
    val simuler: Boolean,
    val behandlinger: List<Long>
)
