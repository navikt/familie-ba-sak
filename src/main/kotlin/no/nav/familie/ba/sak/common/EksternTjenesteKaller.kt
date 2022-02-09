package no.nav.familie.ba.sak.common

import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonException
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.getDataOrThrow
import org.slf4j.LoggerFactory
import org.springframework.web.client.HttpClientErrorException
import java.net.URI

val logger = LoggerFactory.getLogger("eksternTjenesteKaller")
inline fun <reified Data> kallEksternTjeneste(
    tjeneste: String,
    uri: URI,
    formål: String,
    eksterntKall: () -> Ressurs<Data>,
): Data {
    val eksternTjenesteKallerMelding = "[tjeneste=$tjeneste, uri=$uri]"
    logger.info("$eksternTjenesteKallerMelding $formål")

    return try {
        eksterntKall().also { assertGenerelleSuksessKriterier(it) }.getDataOrThrow().also {
            logger.info("$eksternTjenesteKallerMelding Kall ok")
        }
    } catch (exception: Exception) {
        logger.info("$eksternTjenesteKallerMelding Kall feilet: ${exception.message}")
        if (exception is HttpClientErrorException.Forbidden) throw exception

        throw IntegrasjonException("Kall mot $tjeneste feilet: ${exception.message}", exception)
    }
}
