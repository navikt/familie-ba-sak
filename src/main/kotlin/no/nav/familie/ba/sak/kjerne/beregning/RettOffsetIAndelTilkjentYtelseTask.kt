package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
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
        val behandlingerMedFeilaktigeOffsets =
            behandlingRepository.finnBehandlingerMedDuplikateOffsetsForAndelTilkjentYtelse()
                .map { behandlingRepository.finnBehandling(it) }

        val sisteISinFagsak = behandlingerMedFeilaktigeOffsets
            .map { it.fagsak }
            .map { behandlingRepository.finnBehandlinger(it.id).sortedBy { behandling -> behandling.id } }
            .map { it.last() }
            .filter { behandlingerMedFeilaktigeOffsets.contains(it) }

        sisteISinFagsak
            .forEach {
                /**
                 *
                 * det vi skal her: rette opp offset i aty for denne behandlinga
                 * som allereie er sendt til økonomi
                 * dvs eigentleg finne offsets på nytt for denne, som om han aldri var laga
                 * kan dermed ikkje utan vidare gjenbruke finn siste offset på fagsak, det må vi jo eigentleg gjera for forrige behandling
                 *
                 * får no henta sisteOffsetPåFagsak på bra vis
                 * men hjelper det?
                 * cluet er vel å rekne ut offset for alle i denne nye behandlinga
                 *
                 */

                val oppdatertTilstand =
                    beregningService.hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(behandlingId = it.id)

                val oppdaterteKjeder = ØkonomiUtils.kjedeinndelteAndeler(oppdatertTilstand)

                val forrigeBehandling =
                    behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(behandling = it)
                        ?: error("Finner ikke forrige behandling ved oppdatering av tilkjent ytelse og iverksetting av vedtak")

                val forrigeTilstand =
                    beregningService.hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(forrigeBehandling.id)
                val forrigeKjeder = ØkonomiUtils.kjedeinndelteAndeler(forrigeTilstand)

                val sisteOffsetPerIdent = beregningService.hentSisteOffsetPerIdent(forrigeBehandling.fagsak.id)

                val sisteOffsetPåFagsak =
                    beregningService.hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(forrigeBehandling.id)
                        .finnSisteOffset()

                if (oppdatertTilstand.isNotEmpty()) {
                    val beståendeAndelerMedOppdatertOffset = ØkonomiUtils.finnBeståendeAndelerMedOppdatertOffset(
                        oppdaterteKjeder = oppdaterteKjeder,
                        forrigeKjeder = forrigeKjeder
                    )
                    beståendeAndelerMedOppdatertOffset.forEach { oppdatering ->
                        oppdatering.oppdater()
                        andelTilkjentYtelseRepository.save(oppdatering.beståendeIOppdatert)
                    }
                    // TODO: logg alle endringar her
                    //      val tilkjentYtelseMedOppdaterteAndeler = oppdatertTilstand.first().tilkjentYtelse
                    //    beregningService.lagreTilkjentYtelseMedOppdaterteAndeler(tilkjentYtelseMedOppdaterteAndeler)
                }
            }

        val diff = behandlingerMedFeilaktigeOffsets.minus(sisteISinFagsak.toSet())
        if (diff.isNotEmpty()) secureLogger.warn("Behandlinger med feilaktige offsets, der fagsaka har fått ei nyere behandling: $diff")
    }

    companion object {
        const val TASK_STEP_TYPE = "rettOffsetIAndelTilkjentYtelseTask"
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}
