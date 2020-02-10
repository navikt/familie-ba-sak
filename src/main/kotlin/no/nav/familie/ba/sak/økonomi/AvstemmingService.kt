package no.nav.familie.ba.sak.økonomi

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class AvstemmingService(val økonomiKlient: ØkonomiKlient, val behandlingService: BehandlingService) {

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

    fun konsistensavstemOppdrag(avstemmingsdato: LocalDateTime, utbetalingsoppdrag: List<Utbetalingsoppdrag>) {

        // TODO: Bytt ut Liste av utbetalingsoppdrag med disse oppdragsId-ene. Tror kanskje at vi ikke trenger å lagre det på payloaden på tasken.
        val oppdragTilAvstemming = behandlingService.hentAktiveBehandlingerForLøpendeFagsaker()

        Result.runCatching { økonomiKlient.konsistensavstemOppdrag(avstemmingsdato, utbetalingsoppdrag) }
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



    companion object {
        val LOG: Logger = LoggerFactory.getLogger(AvstemmingService::class.java)
    }
}
