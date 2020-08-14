package no.nav.familie.ba.sak.infotrygd

import no.nav.commons.foedselsnummer.FoedselsNr
import no.nav.familie.http.client.AbstractRestClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestOperations
import java.lang.Exception
import java.net.URI

@Component
class InfotrygdBarnetrygdClient(@Value("\${FAMILIE_BA_INFOTRYGD_BARNETRYGD_API_URL}") private val clientUri: URI,
                                @Qualifier("jwtBearer") restOperations: RestOperations,
                                private val environment: Environment)
    : AbstractRestClient(restOperations, "infotrygd_barnetrygd") {

    fun harLøpendeInfotrygdsak(søkersIdenter: List<String>, barnasIdenter: List<String>): Boolean {
        if (environment.activeProfiles.contains("e2e")) {
            return true
        }

        val uri = URI.create("$clientUri/infotrygd/barnetrygd/lopendeSak")

        val request = InfotrygdSøkRequest(søkersIdenter.map { FoedselsNr(it) }, barnasIdenter.map { FoedselsNr(it) })

        return try {
            !postForEntity<InfotrygdSøkResponse>(uri, request).ingenTreff
        } catch (ex: Exception) {
            when (ex) {
                is HttpClientErrorException -> secureLogger.error("Http feil mot infotrygd barnetrygd: httpkode: ${ex.statusCode}, feilmelding ${ex.message}", ex)
                else -> secureLogger.error("Feil mot infotrygd-barnetrygd; melding ${ex.message}", ex)
            }
            logger.error("Feil mot Infotrygd-barnetrygd.")
            throw ex
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(this::class.java)
        val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}