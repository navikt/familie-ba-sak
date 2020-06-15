package no.nav.familie.ba.sak.infotrygd

import no.nav.familie.http.client.AbstractRestClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestOperations
import java.lang.Exception
import java.net.URI

@Component
class InfotrygdBarnetrygdClient(@Value("\${FAMILIE_BA_INFOTRYGD_BARNETRYGD_API_URL}") private val clientUri: URI,
                                @Qualifier("jwtBearer") restOperations: RestOperations)
    : AbstractRestClient(restOperations, "infotrygd_barnetrygd") {

    fun foo(dto: String): Int {
        val uri = URI.create("$clientUri/infotrygd/barnetrygd/personsok")

        return try {
            postForEntity<String>(uri, dto) // String, Int, foo er midlertidig.
            2
        } catch (ex: Exception) {
            when (ex) {
                is HttpClientErrorException -> logger.error("Http feil mot infotrygd barnetrygd: httpkode: ${ex.statusCode}, feilmelding ${ex.message}", ex)
                else -> logger.error("Feil mot infotrygd feed; melding ${ex.message}", ex)
            }
            throw ex
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }
}