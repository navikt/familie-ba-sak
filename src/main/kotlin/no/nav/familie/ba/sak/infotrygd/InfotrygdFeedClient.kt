package no.nav.familie.ba.sak.infotrygd

import no.nav.familie.ba.sak.infotrygd.domene.InfotrygdFødselhendelsesFeedDto
import no.nav.familie.ba.sak.infotrygd.domene.InfotrygdVedtakFeedDto
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.Ressurs
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestOperations
import java.io.IOException
import java.net.URI

@Component
class InfotrygdFeedClient(@Value("\${FAMILIE_BA_INFOTRYGD_FEED_API_URL}") private val clientUri: URI,
                          @Qualifier("jwtBearer") restOperations: RestOperations,
                          private val environment: Environment)
    : AbstractRestClient(restOperations, "infotrygd_feed") {

    fun sendFødselhendelsesFeedTilInfotrygd(infotrygdFødselhendelsesFeedDto: InfotrygdFødselhendelsesFeedDto) {
        return try {
            sendFeedTilInfotrygd(URI.create("$clientUri/barnetrygd/v1/feed/foedselsmelding"), infotrygdFødselhendelsesFeedDto)
        } catch (e: Exception) {
            loggOgKastException(e)
        }
    }

    fun sendVedtakFeedTilInfotrygd(infotrygdVedtakFeedDto: InfotrygdVedtakFeedDto) {
        try {
            sendFeedTilInfotrygd(URI.create("$clientUri/barnetrygd/v1/feed/vedtaksmelding"), infotrygdVedtakFeedDto)
        } catch (e: Exception) {
            loggOgKastException(e)
        }
    }

    private fun loggOgKastException(e: Exception) {
        if (e is HttpClientErrorException) {
            logger.error("Http feil mot infotrygd feed: httpkode: ${e.statusCode}, feilmelding ${e.message}", e)
        } else {
            logger.error("Feil mot infotrygd feed; melding ${e.message}", e)
        }

        throw e
    }


    @Retryable(value = [IOException::class], maxAttempts = 3, backoff = Backoff(delayExpression = "\${retry.backoff.delay:5000}"), )
    private fun sendFeedTilInfotrygd(endpoint: URI, feed: Any) {
        if (environment.activeProfiles.contains("e2e")) {
            return
        }
        postForEntity<Ressurs<String>>(endpoint, feed)
    }

    companion object {

        val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }
}