package no.nav.familie.ba.sak.infotrygd

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.RessursUtils.assertGenerelleSuksessKriterier
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.oppgave.OppgaveResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestOperations
import java.net.URI

@Component
class InfotrygdFeedClient(@Value("\${FAMILIE_BA_INFOTRYGD_FEED_API_URL}") private val clientUri: URI,
                          @Qualifier("jwtBearer") restOperations: RestOperations)
    : AbstractRestClient(restOperations, "infotrygd_feed") {

    fun leggTilInfotrygdFeed(infotrygdFeedDto: InfotrygdFeedDto) {
        val uri = URI.create("$clientUri/barnetrygd/v1/feed/foedselsmelding")

        return Result.runCatching {
            postForEntity<Ressurs<String>>(uri, infotrygdFeedDto)
        }.fold(
                onSuccess = {
                },
                onFailure = {
                    logger.error("Feil mot infotrygd feed", it)
                    val message = if (it is RestClientResponseException) it.responseBodyAsString else ""

                    throw Feil(message = "Infotrygd Feed kunne ikke bli opprettet $message",
                            frontendFeilmelding = "Feilet på fødselsnummer ${infotrygdFeedDto.fnrBarn}",
                            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                            throwable = it
                    )
                }
        )
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }
}