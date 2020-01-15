package no.nav.familie.ba.sak.økonomi

import no.nav.familie.kontrakter.felles.Ressurs
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class AvstemmingService(val økonomiKlient: ØkonomiKlient) {

    fun avstemOppdrag(fraDato: LocalDateTime, tilDato: LocalDateTime): Ressurs<String> {

        Result.runCatching { økonomiKlient.avstemOppdrag(fraDato, tilDato) }
                .fold(
                        onSuccess = {
                            return Ressurs.success("Avstemming OK")
                        },
                        onFailure = {
                            LOG.warn("Avstemming oppdrag feilet")
                            throw it
                        }
                )
    }

    companion object {
        val LOG = LoggerFactory.getLogger(AvstemmingService::class.java)
    }
}