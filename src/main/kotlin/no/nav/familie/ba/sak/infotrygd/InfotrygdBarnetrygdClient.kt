package no.nav.familie.ba.sak.infotrygd

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.ba.infotrygd.InfotrygdSøkRequest
import no.nav.familie.kontrakter.ba.infotrygd.InfotrygdSøkResponse
import no.nav.familie.kontrakter.ba.infotrygd.Sak
import no.nav.familie.kontrakter.ba.infotrygd.Stønad
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestOperations
import java.net.URI

@Component
class InfotrygdBarnetrygdClient(@Value("\${FAMILIE_BA_INFOTRYGD_BARNETRYGD_API_URL}") private val clientUri: URI,
                                @Qualifier("jwtBearer") restOperations: RestOperations,
                                private val environment: Environment)
    : AbstractRestClient(restOperations, "infotrygd_barnetrygd") {

    fun harLøpendeSakIInfotrygd(søkersIdenter: List<String>, barnasIdenter: List<String> = emptyList()): Boolean {
        if (environment.activeProfiles.contains("e2e")) {
            return false
        }

        val uri = URI.create("$clientUri/infotrygd/barnetrygd/lopendeSak")

        val request = InfotrygdSøkRequest(søkersIdenter, barnasIdenter)

        return try {
            postForEntity<InfotrygdTreffResponse>(uri, request).ingenTreff.not()
        } catch (ex: Exception) {
            loggFeil(ex, uri)
            throw ex
        }
    }

    fun hentSaker(søkersIdenter: List<String>, barnasIdenter: List<String> = emptyList()): InfotrygdSøkResponse<Sak> {
        if (environment.activeProfiles.contains("e2e")) return InfotrygdSøkResponse(emptyList(), emptyList())

        val uri = URI.create("$clientUri/infotrygd/barnetrygd/saker")

        return try {
            postForEntity(uri, InfotrygdSøkRequest(søkersIdenter, barnasIdenter))
        } catch (ex: Exception) {
            loggFeil(ex, uri)
            throw Feil(message = "Henting av infotrygdsaker feilet. Gav feil: ${ex.message}",
                       frontendFeilmelding = "Henting av infotrygdsaker feilet.",
                       httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                       throwable = ex)
        }
    }

    fun hentStønader(søkersIdenter: List<String>, barnasIdenter: List<String>): InfotrygdSøkResponse<Stønad> {
        if (environment.activeProfiles.contains("e2e")) return InfotrygdSøkResponse(emptyList(), emptyList())

        val uri = URI.create("$clientUri/infotrygd/barnetrygd/stonad")

        return try {
            postForEntity(uri, InfotrygdSøkRequest(søkersIdenter, barnasIdenter))
        } catch (ex: Exception) {
            loggFeil(ex, uri)
            throw Feil(message = "Henting av infotrygdstønader feilet. Gav feil: ${ex.message}",
                    frontendFeilmelding = "Henting av infotrygdstønader feilet.",
                    httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                    throwable = ex)
        }
    }

    private fun loggFeil(ex: Exception, uri: URI) {
        when (ex) {
            is HttpClientErrorException -> secureLogger.error("Http feil mot ${uri.path}: httpkode: ${ex.statusCode}, feilmelding ${ex.message}",
                                                              ex)
            else -> secureLogger.error("Feil mot ${uri.path}; melding ${ex.message}", ex)
        }
        logger.error("Feil mot ${uri.path}.")
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(this::class.java)
        val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}
