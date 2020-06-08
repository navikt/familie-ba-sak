package no.nav.familie.ba.sak.infotrygd

import no.nav.familie.ba.sak.integrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.Ressurs
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
class InfotrygdFeedClient(@Value("\${FAMILIE_BA_INFOTRYGD_FEED_API_URL}") private val clientUri: URI,
                          @Qualifier("jwtBearer") restOperations: RestOperations,
                          private val environment: Environment)
    : AbstractRestClient(restOperations, "infotrygd_feed") {

    fun leggTilInfotrygdFeed(infotrygdFeedDto: InfotrygdFeedDto) {
        if (environment.activeProfiles.contains("e2e")) {
            return
        }

        val uri = URI.create("$clientUri/barnetrygd/v1/feed/foedselsmelding")

        return Result.runCatching {
            postForEntity<Ressurs<String>>(uri, infotrygdFeedDto)
        }.fold(
                onSuccess = {
                },
                onFailure = {
                    if(it is HttpClientErrorException) {
                        logger.error("Http feil mot infotrygd feed: httpkode: ${it.statusCode}, feilmelding ${it.message}", it)
                    } else {
                        logger.error("Feil mot infotrygd feed; melding ${it.message}", it)
                    }

                    throw it
                }
        )
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }
}