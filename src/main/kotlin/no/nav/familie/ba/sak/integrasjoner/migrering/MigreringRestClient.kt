package no.nav.familie.ba.sak.integrasjoner.migrering

import no.nav.familie.ba.sak.integrasjoner.infotrygd.domene.MigreringResponseDto
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.getDataOrThrow
import no.nav.familie.kontrakter.felles.objectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestOperations
import java.net.URI

@Component
class MigreringRestClient(
    @Value("\${FAMILIE_BA_MIGRERING_API_URL}") private val clientUri: URI,
    @Qualifier("jwtBearerClientCredentials") restOperations: RestOperations
) : AbstractRestClient(restOperations, "migrering") {

    fun migrertAvSaksbehandler(personIdent: String, migreringResponseDto: MigreringResponseDto): String {
        val uri = URI.create("$clientUri/migrer/migrert-av-saksbehandler")
        val body = MigrertAvSaksbehandlerRequest(personIdent, migreringResponseDto)
        return try {
            val response: Ressurs<String> = postForEntity(uri, body)
            response.getDataOrThrow()
        } catch (ex: Exception) {
            val bodyAsString = objectMapper.writeValueAsString(body)
            when (ex) {
                is HttpClientErrorException -> secureLogger.error(
                    "Http feil mot ${uri.path}: httpkode=${ex.statusCode}, feilmelding=${ex.message}, body=$bodyAsString",
                    ex
                )
                else -> secureLogger.error("Feil mot ${uri.path}, melding=${ex.message}, body=$bodyAsString", ex)
            }
            logger.warn("Feil ved kall mot ba-migrering ${uri.path}")
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
