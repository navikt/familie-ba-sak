package no.nav.familie.ba.sak.integrasjoner.infotrygd

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.ekstern.bisys.BisysUtvidetBarnetrygdResponse
import no.nav.familie.ba.sak.ekstern.pensjon.BarnetrygdTilPensjonRequest
import no.nav.familie.ba.sak.ekstern.pensjon.BarnetrygdTilPensjonResponse
import no.nav.familie.ba.sak.integrasjoner.retryVedException
import no.nav.familie.kontrakter.ba.infotrygd.InfotrygdSøkRequest
import no.nav.familie.kontrakter.ba.infotrygd.InfotrygdSøkResponse
import no.nav.familie.kontrakter.ba.infotrygd.Sak
import no.nav.familie.kontrakter.ba.infotrygd.Stønad
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.retry.RetryException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth

@Component
class InfotrygdBarnetrygdKlient(
    @Value("\${FAMILIE_BA_INFOTRYGD_API_URL}") private val klientUri: URI,
    @Qualifier("infotrygdBarnetrygdRestClient") private val restClient: RestClient,
    @Value("$RETRY_BACKOFF_3_MIN") private val retryBackoffDelay: Long,
) {
    fun hentSaker(
        søkersIdenter: List<String>,
        barnasIdenter: List<String> = emptyList(),
    ): InfotrygdSøkResponse<Sak> {
        val uri = URI.create("$klientUri/infotrygd/barnetrygd/saker")

        return try {
            restClient
                .post()
                .uri(uri)
                .body(InfotrygdSøkRequest(søkersIdenter, barnasIdenter))
                .retrieve()
                .body()!!
        } catch (ex: Exception) {
            loggFeil(ex, uri)
            throw Feil(
                message = "Henting av infotrygdsaker feilet. Gav feil: ${ex.message}",
                frontendFeilmelding = "Henting av infotrygdsaker feilet.",
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                throwable = ex,
            )
        }
    }

    fun hentStønader(
        søkersIdenter: List<String>,
        barnasIdenter: List<String>,
        historikk: Boolean = false,
    ): InfotrygdSøkResponse<Stønad> {
        val uri = URI.create("$klientUri/infotrygd/barnetrygd/stonad?historikk=$historikk")

        return try {
            restClient
                .post()
                .uri(uri)
                .body(InfotrygdSøkRequest(søkersIdenter, barnasIdenter))
                .retrieve()
                .body()!!
        } catch (ex: Exception) {
            loggFeil(ex, uri)
            throw Feil(
                message = "Henting av infotrygdstønader feilet. Gav feil: ${ex.message}",
                frontendFeilmelding = "Henting av infotrygdstønader feilet.",
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                throwable = ex,
            )
        }
    }

    data class HentUtvidetBarnetrygdRequest(
        val personIdent: String,
        val fraDato: YearMonth,
    )

    fun hentUtvidetBarnetrygd(
        personIdent: String,
        fraDato: YearMonth,
    ): BisysUtvidetBarnetrygdResponse {
        val uri = URI.create("$klientUri/infotrygd/barnetrygd/utvidet")
        val body = HentUtvidetBarnetrygdRequest(personIdent, fraDato)
        return try {
            retryVedException(retryBackoffDelay).execute {
                restClient
                    .post()
                    .uri(uri)
                    .body(body)
                    .retrieve()
                    .body()!!
            }
        } catch (ex: Exception) {
            val lastException = if (ex is RetryException) ex.lastException else ex

            loggFeil(lastException, uri)
            throw RuntimeException("Henting av utvidet barnetrygd feilet. Gav feil: ${lastException.message}", lastException)
        }
    }

    fun hentBarnetrygdTilPensjon(
        personIdent: String,
        fraDato: LocalDate,
    ): BarnetrygdTilPensjonResponse {
        val uri = URI.create("$klientUri/infotrygd/barnetrygd/pensjon")
        val body = BarnetrygdTilPensjonRequest(personIdent, fraDato)
        return try {
            restClient
                .post()
                .uri(uri)
                .body(body)
                .retrieve()
                .body()!!
        } catch (ex: Exception) {
            loggFeil(ex, uri)
            throw RuntimeException("Henting av barnetrygd til pensjon feilet. Gav feil: ${ex.message}", ex)
        }
    }

    fun hentPersonerMedBarnetrygdTilPensjon(år: Int): List<String> {
        val uri = URI.create("$klientUri/infotrygd/barnetrygd/pensjon?aar=$år")
        return try {
            retryVedException(Duration.ofMinutes(3).toMillis()).execute {
                restClient
                    .get()
                    .uri(uri)
                    .retrieve()
                    .body()!!
            }
        } catch (ex: Exception) {
            loggFeil(ex, uri)
            throw RuntimeException("Henting av personer med barnetrygd feilet. Gav feil: ${ex.message}", ex)
        }
    }

    private fun loggFeil(
        ex: Throwable,
        uri: URI,
    ) {
        when (ex) {
            is HttpClientErrorException -> {
                secureLogger.warn(
                    "Http feil mot ${uri.path}: httpkode: ${ex.statusCode}, feilmelding ${ex.message}",
                    ex,
                )
            }

            else -> {
                secureLogger.warn("Feil mot ${uri.path}; melding ${ex.message}", ex)
            }
        }
        logger.warn("Feil mot ${uri.path}.")
    }

    companion object {
        const val RETRY_BACKOFF_3_MIN = "\${retry.backoff.delay:180000}"
        private val logger: Logger = LoggerFactory.getLogger(InfotrygdBarnetrygdKlient::class.java)
    }
}
