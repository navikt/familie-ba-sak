package no.nav.familie.ba.sak.økonomi

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.beregning.BeregningService
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
                            LOG.debug("Grensesnittavstemming mot oppdrag utført.")
                        },
                        onFailure = {
                            LOG.error("Grensesnittavstemming mot oppdrag feilet", it)
                            throw it
                        }
                )
    }

    fun konsistensavstemOppdrag(avstemmingsdato: LocalDateTime) {

        val oppdragTilAvstemming = behandlingService.hentOppdragIderTilKonsistensavstemming()
        LOG.info("Utfører konsistensavstemming for ${oppdragTilAvstemming.size} løpende saker")

        Result.runCatching { økonomiKlient.konsistensavstemOppdrag(avstemmingsdato, oppdragTilAvstemming) }
                .fold(
                        onSuccess = {
                            LOG.debug("Konsistensavstemming mot oppdrag utført.")
                        },
                        onFailure = {
                            LOG.error("Konsistensavstemming mot oppdrag feilet", it)
                            throw it
                        }
                )
    }

    fun konsistensavstemOppdragV2(avstemmingsdato: LocalDateTime) {

        val perioderTilAvstemming = hentDataForKonsistensavstemming()

        LOG.info("Utfører konsisensavstemming for ${perioderTilAvstemming.size} løpende saker")

        Result.runCatching { økonomiKlient.konsistensavstemOppdragV2(avstemmingsdato, perioderTilAvstemming) }
                .fold(
                        onSuccess = {
                            LOG.debug("Konsistensavstemming mot oppdrag utført.")
                        },
                        onFailure = {
                            LOG.error("Konsistensavstemming mot oppdrag feilet", it)
                            throw it
                        }
                )
    }

    fun hentDataForKonsistensavstemming(): List<PerioderForBehandling> {
        val relevanteBehandlinger = behandlingService.finnSisteIverksatteBehandlingFraLøpendeFagsaker()
        return relevanteBehandlinger
                .map { behandlingId ->
                    val andelerIRelevantBehandling = beregningService.hentAndelerTilkjentYtelseForBehandling(behandlingId)
                    andelerIRelevantBehandling
                            .groupBy { it.kildeBehandlingId }
                            .map { (kildeBehandlingId, andeler) ->
                                PerioderForBehandling(behandlingId = kildeBehandlingId.toString(),
                                                      perioder = andeler
                                                              .map {
                                                                  it.periodeOffset
                                                                  ?: error("Andel ${it.id} på iverksatt behandling på løpende fagsak mangler periodeOffset")
                                                              }
                                                              .toSet())
                            }
                }
                .flatten()
    }


    companion object {

        val LOG: Logger = LoggerFactory.getLogger(AvstemmingService::class.java)
    }
}
