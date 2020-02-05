package no.nav.familie.ba.sak.økonomi

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class AvstemmingService(val økonomiKlient: ØkonomiKlient) {

    fun avstemOppdrag(fraDato: LocalDateTime, tilDato: LocalDateTime) {

        Result.runCatching { økonomiKlient.avstemOppdrag(fraDato, tilDato) }
                .fold(
                        onSuccess = {
                            LOG.debug("Avstemming mot oppdrag utført.")
                        },
                        onFailure = {
                            LOG.error("Avstemming mot oppdrag feilet", it)
                            throw it
                        }
                )
    }

    companion object {
        val LOG: Logger = LoggerFactory.getLogger(AvstemmingService::class.java)
    }
}
