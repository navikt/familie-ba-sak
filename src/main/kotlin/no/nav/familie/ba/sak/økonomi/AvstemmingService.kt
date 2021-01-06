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

    private fun hentDataForKonsistensavstemming(): List<PerioderForBehandling> {
        val relevanteBehandlinger = behandlingService.hentSisteIverksatteBehandlingerFraLøpendeFagsaker()
        return relevanteBehandlinger
                .chunked(1000)
                .map { chunk ->
                    val relevanteAndeler = beregningService.hentAndelerTilkjentYtelseForBehandlinger(chunk)
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

        val LOG: Logger = LoggerFactory.getLogger(AvstemmingService::class.java)
    }
}
