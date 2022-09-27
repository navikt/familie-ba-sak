package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.integrasjoner.økonomi.OffsetOppdatering
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
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
        loggBehandlingIder("Behandlinger med feilaktige offsets", behandlingerMedFeilaktigeOffsets.map { it.id })

        val relevanteBehandlinger = finnRelevanteBehandlingerForOppdateringAvOffset(behandlingerMedFeilaktigeOffsets)
        loggBehandlingIder("Relevante behandlinger", relevanteBehandlinger.map { it.id })

        val behandlingIderSomIkkeKanOppdateres = mutableListOf<Long>()

        relevanteBehandlinger
            .forEach {
                val andelerSomSendesTilOppdrag =
                    beregningService.hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(behandlingId = it.id)

                val forrigeBehandling =
                    behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(behandling = it)

                if (andelerSomSendesTilOppdrag.isNotEmpty() && forrigeBehandling != null) {
                    val andelerFraForrigeBehandling =
                        beregningService.hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(forrigeBehandling.id)

                    val beståendeAndelerMedOppdatertOffset = ØkonomiUtils.finnBeståendeAndelerMedOppdatertOffset(
                        oppdaterteKjeder = ØkonomiUtils.kjedeinndelteAndeler(andelerSomSendesTilOppdrag),
                        forrigeKjeder = ØkonomiUtils.kjedeinndelteAndeler(andelerFraForrigeBehandling)
                    )

                    val logglinjer =
                        beståendeAndelerMedOppdatertOffset.joinToString(separator = System.lineSeparator()) { oppdatering ->
                            formaterLogglinje(
                                oppdatering
                            )
                        }
                    secureLogger.info(
                        "Behandling: $it," +
                            "\nLogglinjer: " +
                            "\n$logglinjer"
                    )

                    if (!payload.simuler) {
                        beståendeAndelerMedOppdatertOffset.forEach { oppdatering -> oppdatering.oppdater() }
                    }
                } else {
                    if (andelerSomSendesTilOppdrag.isEmpty()) {
                        secureLogger.warn("Fant ingen andeler som skal sendes til oppdrag for behandling $it")
                    }
                    if (forrigeBehandling == null) {
                        secureLogger.warn("Fant ikke forrige behandling for behandling $it")
                    }
                    behandlingIderSomIkkeKanOppdateres.add(it.id)
                }
            }

        val behandlingIderSomFaktiskBleOppdatert =
            relevanteBehandlinger.map { it.id }.minus(behandlingIderSomIkkeKanOppdateres)

        loggBehandlingIder("Behandlinger som ble oppdatert", behandlingIderSomFaktiskBleOppdatert)
        loggBehandlingIder(
            "Behandlinger som var relevant, men ikke kunne oppdateres",
            behandlingIderSomIkkeKanOppdateres
        )

        val behandlingerMedNyereBehandlingSomErAvsluttet =
            behandlingerMedFeilaktigeOffsets.minus(relevanteBehandlinger)
        if (behandlingerMedNyereBehandlingSomErAvsluttet.isNotEmpty()) {
            loggBehandlingIder(
                "Behandlinger med feilaktige offsets, der fagsaka har fått ei nyere behandling som er avslutta",
                behandlingerMedNyereBehandlingSomErAvsluttet.map { it.id }
            )
        }
    }

    fun finnRelevanteBehandlingerForOppdateringAvOffset(behandlingerMedFeilaktigeOffsets: List<Behandling>) =
        behandlingerMedFeilaktigeOffsets.filter { behandling ->
            val alleBehandlingerPåFagsak =
                behandlingHentOgPersisterService.hentBehandlinger(fagsakId = behandling.fagsak.id)

            val behandlingerOpprettetEtterDenneBehandlingen =
                alleBehandlingerPåFagsak.filter { it.opprettetTidspunkt.isAfter(behandling.opprettetTidspunkt) && !it.erHenlagt() }

            val finnesUgyldigBehandlingEtterDenne =
                behandlingerOpprettetEtterDenneBehandlingen.filter { it.status == BehandlingStatus.AVSLUTTET }
                    .isNotEmpty()

            if (finnesUgyldigBehandlingEtterDenne) {
                secureLogger.warn("Behandling $behandling blir ikke oppdatert fordi det finnes senere behandling som er avsluttet")
            }

            !finnesUgyldigBehandlingEtterDenne
        }

    private fun loggBehandlingIder(tekst: String, behandlingIder: List<Long>) {
        logger.warn(
            "$tekst (${behandlingIder.size} stk): ${
            behandlingIder.joinToString(separator = ",")
            }"
        )
    }

    private fun formaterLogglinje(oppdatering: OffsetOppdatering) =
        "Oppdaterer andel tilkjent ytelse ${oppdatering.beståendeAndelSomSkalHaOppdatertOffset} med periodeoffset=${oppdatering.periodeOffset}, forrigePeriodeOffset=${oppdatering.forrigePeriodeOffset} og kildeBehandlingId=${oppdatering.kildeBehandlingId}"

    companion object {
        const val TASK_STEP_TYPE = "rettOffsetIAndelTilkjentYtelseTask"
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
        private val logger = LoggerFactory.getLogger(RettOffsetIAndelTilkjentYtelseTask::class.java)
    }
}

data class RettOffsetIAndelTilkjentYtelseDto(
    val simuler: Boolean,
    val behandlinger: Set<Long>
)
