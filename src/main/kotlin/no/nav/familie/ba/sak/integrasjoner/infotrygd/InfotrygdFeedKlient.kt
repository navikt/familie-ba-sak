package no.nav.familie.ba.sak.integrasjoner.infotrygd

import no.nav.familie.ba.sak.integrasjoner.infotrygd.domene.InfotrygdFødselhendelsesFeedDto
import no.nav.familie.ba.sak.integrasjoner.infotrygd.domene.InfotrygdVedtakFeedDto
import no.nav.familie.ba.sak.integrasjoner.infotrygd.domene.StartBehandlingDto
import no.nav.familie.ba.sak.integrasjoner.retryVedException
import no.nav.familie.ba.sak.integrasjoner.retryVedIOException
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.restklient.client.AbstractRestClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestOperations
import java.net.URI

@Component
class InfotrygdFeedKlient(
    @Value("\${FAMILIE_BA_INFOTRYGD_FEED_API_URL}") private val klientUri: URI,
    @Qualifier("jwtBearer") restOperations: RestOperations,
) : AbstractRestClient(restOperations, "infotrygd_feed") {
    fun sendFødselhendelsesFeedTilInfotrygd(infotrygdFødselhendelsesFeedDto: InfotrygdFødselhendelsesFeedDto) =
        try {
            sendFeedTilInfotrygd(
                URI.create("$klientUri/barnetrygd/v1/feed/foedselsmelding"),
                infotrygdFødselhendelsesFeedDto,
            )
        } catch (e: Exception) {
            loggOgKastException(e)
        }

    fun sendVedtakFeedTilInfotrygd(infotrygdVedtakFeedDto: InfotrygdVedtakFeedDto) {
        try {
            sendFeedTilInfotrygd(URI.create("$klientUri/barnetrygd/v1/feed/vedtaksmelding"), infotrygdVedtakFeedDto)
        } catch (e: Exception) {
            loggOgKastException(e)
        }
    }

    fun sendStartBehandlingTilInfotrygd(startBehandlingDto: StartBehandlingDto) {
        try {
            sendFeedTilInfotrygd(
                URI.create("$klientUri/barnetrygd/v1/feed/startbehandlingsmelding"),
                startBehandlingDto,
            )
        } catch (e: Exception) {
            loggOgKastException(e)
        }
    }

    private fun loggOgKastException(e: Exception) {
        if (e is HttpClientErrorException) {
            logger.warn("Http feil mot infotrygd feed: httpkode: ${e.statusCode}, feilmelding ${e.message}", e)
        } else {
            logger.warn("Feil mot infotrygd feed; melding ${e.message}", e)
        }

        throw e
    }

    private fun sendFeedTilInfotrygd(
        endpoint: URI,
        feed: Any,
    ) {
        retryVedIOException(5000).execute {
            postForEntity<Ressurs<String>>(endpoint, feed)
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(InfotrygdFeedKlient::class.java)
    }
}
