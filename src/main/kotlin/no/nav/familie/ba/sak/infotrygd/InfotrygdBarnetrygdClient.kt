package no.nav.familie.ba.sak.infotrygd

import no.nav.familie.http.client.AbstractRestClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
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

    fun hentSaker(søkersIdenter: List<String>, barnasIdenter: List<String>): InfotrygdSøkResponse<SakDto> {
        if (environment.activeProfiles.contains("e2e")) return InfotrygdSøkResponse(emptyList(), emptyList())

        val uri = URI.create("$clientUri/infotrygd/barnetrygd/saker")

        return try {
            postForEntity(uri, InfotrygdSøkRequest(søkersIdenter, barnasIdenter))
        } catch (ex: Exception) {
            loggFeil(ex, uri)
            throw ex
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
