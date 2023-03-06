package no.nav.familie.ba.sak.integrasjoner.økonomi.InternKonsistendsavstemming

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiKlient
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.task.InternKonsistensavstemmingTask
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class InternKonsistensavstemmingService(
    val økonomiKlient: ØkonomiKlient,
    val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    val fagsakRepository: FagsakRepository
) {
    fun validerLikUtbetalingIAndeleneOgUtbetalingsoppdragetPåAlleFagsaker(maksAntallTasker: Int = Int.MAX_VALUE) {
        fagsakRepository.hentIkkeArkiverteFagsaker()
            .map { it.id }
            .sortedBy { it }
            // Størrelse burde være en multippel av 3000 siden vi chunker det i 3000 i familie-oppdrag
            .chunked(3000)
            .take(maksAntallTasker)
            .forEach { InternKonsistensavstemmingTask.opprettTask(it.toSet()) }
    }

    fun validerLikUtbetalingIAndeleneOgUtbetalingsoppdraget(fagsaker: Set<Long>) {
        val fagsakTilSisteUtbetalingsoppdragOgSisteAndelerMap =
            hentFagsakTilSisteUtbetalingsoppdragOgSisteAndelerMap(fagsaker)

        fagsakTilSisteUtbetalingsoppdragOgSisteAndelerMap.forEach { entry ->
            val fagsakId = entry.key
            val andeler = entry.value.first
            val utbetalingsoppdrag = entry.value.second

            val sumUtbetalingFraAndeler = andeler.sumOf { it.kalkulertUtbetalingsbeløp }

            val sumUtbetalingFraUtbetalingsoppdrag =
                utbetalingsoppdrag?.utbetalingsperiode?.sumOf { it.sats } ?: BigDecimal.ZERO

            if (sumUtbetalingFraUtbetalingsoppdrag != sumUtbetalingFraAndeler.toBigDecimal()) {
                logger.error("Fagsak $fagsakId har ulikt utbetalingsbeløp i andelene og utbetalingsoppdraget")
                secureLogger.info(
                    "Fagsak $fagsakId har ulikt utbetalingsbeløp i andelene og utbetalingsoppdraget. " +
                        "\nBetalingen i utbetalingsoppdraget=$sumUtbetalingFraUtbetalingsoppdrag" +
                        "\nBetalingen i andelene=$sumUtbetalingFraAndeler"
                )
            }
        }
    }

    private fun hentFagsakTilSisteUtbetalingsoppdragOgSisteAndelerMap(fagsakIder: Set<Long>): Map<Long, Pair<List<AndelTilkjentYtelse>, Utbetalingsoppdrag?>> {
        val scope = CoroutineScope(SupervisorJob())
        val fagsakTilSisteUtbetalingsoppdragMapDeffered = scope.async {
            hentFagsakTilSissteUtbetalingsoppdragMap(fagsakIder)
        }

        val fagsakTilAndelerISisteVedtatteBehandlingMap = hentFagsakTilAndelerISisteVedtatteBehandlingMap(fagsakIder)

        val fagsakTilSisteUtbetalingsoppdragMap =
            runBlocking { fagsakTilSisteUtbetalingsoppdragMapDeffered.await() }

        val fagsakTilSisteUtbetalingsoppdragOgSisteAndeler =
            fagsakTilAndelerISisteVedtatteBehandlingMap.mapValues { (fagsakId, andel) ->
                Pair(andel, fagsakTilSisteUtbetalingsoppdragMap[fagsakId])
            }
        return fagsakTilSisteUtbetalingsoppdragOgSisteAndeler
    }

    private fun hentFagsakTilSissteUtbetalingsoppdragMap(fagsakIder: Set<Long>) =
        økonomiKlient.hentSisteUtbetalingsoppdragForFagsaker(fagsakIder)
            .associate { it.fagsakId to it.utbetalingsoppdrag }

    private fun hentFagsakTilAndelerISisteVedtatteBehandlingMap(fagsaker: Set<Long>): Map<Long, List<AndelTilkjentYtelse>> {
        val behandlinger = behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtattPerFagsak(fagsaker)

        return andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlinger(behandlinger.map { it.id })
            .groupBy { it.behandlingId }
            .mapKeys { (behandlingId, _) -> behandlinger.find { it.id == behandlingId }?.fagsak?.id!! }
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)!!
    }
}

