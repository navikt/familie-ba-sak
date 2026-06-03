package no.nav.familie.ba.sak.common

import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonException
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.getDataOrThrow
import no.nav.familie.kontrakter.felles.jsonMapper
import org.slf4j.LoggerFactory
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClientResponseException
import tools.jackson.module.kotlin.readValue
import java.net.URI

val eksternTjenesteKallerLogger = LoggerFactory.getLogger("eksternTjenesteKaller")

inline fun <reified Data> kallEksternTjeneste(
    tjeneste: String,
    uri: URI,
    formål: String,
    eksterntKall: () -> Data,
): Data {
    loggEksternKall(tjeneste, uri, formål)

    return try {
        val startTid = System.currentTimeMillis()
        val data = eksterntKall()
        val sluttTid = System.currentTimeMillis()

        eksternTjenesteKallerLogger.info(
            "${lagEksternKallPreMelding(tjeneste, uri)} Kall ok. Dette tok ${sluttTid - startTid} ms.",
        )

        data
    } catch (exception: Exception) {
        throw handleException(exception = exception, tjeneste = tjeneste, uri = uri, formål = formål)
    }
}

inline fun <reified Data> kallEksternTjenesteRessurs(
    tjeneste: String,
    uri: URI,
    formål: String,
    eksterntKall: () -> Ressurs<Data>,
): Data {
    loggEksternKall(tjeneste, uri, formål)

    return try {
        eksterntKall().getDataOrThrow().also {
            eksternTjenesteKallerLogger.info("${lagEksternKallPreMelding(tjeneste, uri)} Kall ok")
        }
    } catch (exception: Exception) {
        throw handleException(exception = exception, tjeneste = tjeneste, uri = uri, formål = formål)
    }
}

inline fun <reified Data> kallEksternTjenesteUtenRespons(
    tjeneste: String,
    uri: URI,
    formål: String,
    eksterntKall: () -> Ressurs<Data>,
) {
    loggEksternKall(tjeneste, uri, formål)

    try {
        eksterntKall().also {
            eksternTjenesteKallerLogger.info("${lagEksternKallPreMelding(tjeneste, uri)} Kall ok")
        }
    } catch (exception: Exception) {
        throw handleException(exception = exception, tjeneste = tjeneste, uri = uri, formål = formål)
    }
}

fun lagEksternKallPreMelding(
    tjeneste: String,
    uri: URI,
) = "[tjeneste=$tjeneste, uri=$uri]"

fun loggEksternKall(
    tjeneste: String,
    uri: URI,
    formål: String,
) {
    eksternTjenesteKallerLogger.info("${lagEksternKallPreMelding(tjeneste, uri)} $formål")
}

fun handleException(
    exception: Exception,
    tjeneste: String,
    uri: URI,
    formål: String,
): Exception =
    when (exception) {
        is HttpClientErrorException -> {
            val melding = lesRessurs(exception)?.melding ?: exception.message

            secureLogger.warn(
                "${
                    lagEksternKallPreMelding(
                        tjeneste,
                        uri,
                    )
                } Kall mot $tjeneste feilet. Formål: $formål. Feilmelding: $melding",
                exception.cause,
            )
            eksternTjenesteKallerLogger.warn(
                "${
                    lagEksternKallPreMelding(
                        tjeneste,
                        uri,
                    )
                } Kall mot $tjeneste feilet. Formål: $formål.",
            )
            exception
        }

        else -> {
            opprettIntegrasjonsException(tjeneste, uri, exception, formål)
        }
    }

fun lesRessurs(e: RestClientResponseException): Ressurs<Any>? =
    try {
        if (e.responseBodyAsString.contains("status")) {
            jsonMapper.readValue<Ressurs<Any>>(e.responseBodyAsString)
        } else {
            null
        }
    } catch (ex: Exception) {
        null
    }

private fun opprettIntegrasjonsException(
    tjeneste: String,
    uri: URI,
    exception: Exception,
    formål: String,
): IntegrasjonException {
    val melding =
        if (exception is RestClientResponseException) {
            lesRessurs(exception)?.melding ?: exception.message
        } else {
            exception.message
        }
    return IntegrasjonException(
        msg = "${lagEksternKallPreMelding(tjeneste, uri)} Kall mot \"$tjeneste\" feilet. Formål: $formål. Feilmelding: $melding",
        uri = uri,
        throwable = exception,
    )
}
