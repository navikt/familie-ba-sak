package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.kontrakter.felles.oppdrag.PerioderForBehandling
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class AvstemmingService(val økonomiKlient: ØkonomiKlient,
                        val behandlingService: BehandlingService,
                        val beregningService: BeregningService) {

    fun grensesnittavstemOppdrag(fraDato: LocalDateTime, tilDato: LocalDateTime) {

        Result.runCatching { økonomiKlient.grensesnittavstemOppdrag(fraDato, tilDato) }
                .fold(
                        onSuccess = {
                            logger.debug("Grensesnittavstemming mot oppdrag utført.")
                        },
                        onFailure = {
                            logger.error("Grensesnittavstemming mot oppdrag feilet", it)
                            throw it
                        }
                )
    }

    fun konsistensavstemOppdrag(avstemmingsdato: LocalDateTime) {

        val perioderTilAvstemming = hentDataForKonsistensavstemming()

        logger.info("Utfører konsisensavstemming for ${perioderTilAvstemming.size} løpende saker")

        Result.runCatching { økonomiKlient.konsistensavstemOppdrag(avstemmingsdato, perioderTilAvstemming) }
                .fold(
                        onSuccess = {
                            logger.debug("Konsistensavstemming mot oppdrag utført.")
                        },
                        onFailure = {
                            logger.error("Konsistensavstemming mot oppdrag feilet", it)
                            throw it
                        }
                )
    }

    private fun hentDataForKonsistensavstemming(): List<PerioderForBehandling> {
        val relevanteBehandlinger = behandlingService.hentSisteIverksatteBehandlingerFraLøpendeFagsaker()
        return relevanteBehandlinger
                .chunked(1000)
                .map { chunk ->
                    val relevanteAndeler = beregningService.hentLøpendeAndelerTilkjentYtelseForBehandlinger(
                            behandlingIder = chunk)
                    relevanteAndeler.groupBy { it.kildeBehandlingId }
                            .map { (kildeBehandlingId, andeler) ->
                                PerioderForBehandling(behandlingId = kildeBehandlingId.toString(),
                                                      perioder = andeler
                                                              .map {
                                                                  it.periodeOffset
                                                                  ?: error("Andel ${it.id} på iverksatt behandling på løpende fagsak mangler periodeOffset")
                                                              }
                                                              .toSet())
                            }
                }.flatten()
    }

    companion object {

        private val logger: Logger = LoggerFactory.getLogger(AvstemmingService::class.java)
    }
}
