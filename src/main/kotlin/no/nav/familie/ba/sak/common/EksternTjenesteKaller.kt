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

    return Result.runCatching {
        eksterntKall().also { assertGenerelleSuksessKriterier(it) }.getDataOrThrow()
    }.fold(
        onSuccess = {
            logger.info("$eksternTjenesteKallerMelding Kall ok")
            it
        },
        onFailure = {
            logger.info("$eksternTjenesteKallerMelding Kall feilet: ${it.message}")
            if (it is HttpClientErrorException.Forbidden) throw it

            throw IntegrasjonException("Kall mot $tjeneste feilet: ${it.message}", it)
        }
    )
}
