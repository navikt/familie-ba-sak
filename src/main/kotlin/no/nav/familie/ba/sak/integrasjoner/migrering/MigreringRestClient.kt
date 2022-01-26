package no.nav.familie.ba.sak.integrasjoner.migrering

import no.nav.familie.ba.sak.integrasjoner.infotrygd.domene.MigreringResponseDto
import no.nav.familie.ba.sak.task.OpprettTaskService.Companion.RETRY_BACKOFF_5000MS
import no.nav.familie.http.client.AbstractRestClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestOperations
import java.net.URI

@Component
class MigreringRestClient(
    @Value("\${FAMILIE_BA_MIGRERING_API_URL}") private val clientUri: URI,
    @Qualifier("jwtBearerClientCredentials") restOperations: RestOperations
) : AbstractRestClient(restOperations, "infotrygd") {

    @Retryable(
        value = [RuntimeException::class],
        maxAttempts = 3,
        backoff = Backoff(delayExpression = RETRY_BACKOFF_5000MS)
    )
    fun migrertAvSaksbehandler(personIdent: String, migreringResponseDto: MigreringResponseDto): String {
        val uri = URI.create("$clientUri/migrer/migrert-av-saksbehandler")
        val body = MigrertAvSaksbehandlerRequest(personIdent, migreringResponseDto)
        return try {
            postForEntity(uri, body)
        } catch (ex: Exception) {
            when (ex) {
                is HttpClientErrorException -> secureLogger.error(
                    "Http feil mot ${uri.path}: httpkode: ${ex.statusCode}, feilmelding ${ex.message}",
                    ex
                )
                else -> secureLogger.error("Feil mot ${uri.path}; melding ${ex.message}", ex)
            }
            logger.warn("Feil mot migrering ${uri.path}")
            throw RuntimeException(
                "Feil ved oppdatering av migrering trigget av saksbehandler. Gav feil: ${ex.message}",
                ex
            )
        }
    }

    data class MigrertAvSaksbehandlerRequest(val personIdent: String, val migreringsResponse: MigreringResponseDto)

    companion object {

        private val logger: Logger = LoggerFactory.getLogger(MigreringRestClient::class.java)
    }
}
