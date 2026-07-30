package no.nav.familie.ba.sak.config

import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.felles.tokenklient.entraid.EntraIDRestClientFactory
import no.nav.familie.log.interceptor.ConsumerIdClientInterceptor
import no.nav.familie.log.interceptor.MdcValuesPropagatingClientInterceptor
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.time.Duration

@Configuration
class RestClientConfig(
    private val entraIDRestClientFactory: EntraIDRestClientFactory,
    private val consumerIdClientInterceptor: ConsumerIdClientInterceptor,
    private val mdcValuesPropagatingClientInterceptor: MdcValuesPropagatingClientInterceptor,
) {
    @Bean("integrasjonerRestClient")
    fun integrasjonerRestClient(
        @Value("\${FAMILIE_INTEGRASJONER_SCOPE}") scope: String,
    ): RestClient =
        entraIDRestClientFactory
            .lagHybridRestKlient(scope) { SikkerhetContext.hentJwt()?.tokenValue }
            .medJsonContentType()

    @Bean("integrasjonerM2mRestClient")
    fun integrasjonerM2mRestClient(
        @Value("\${FAMILIE_INTEGRASJONER_SCOPE}") scope: String,
    ): RestClient =
        entraIDRestClientFactory
            .lagMaskinTilMaskinRestKlient(scope)
            .medJsonContentType()

    @Bean("pdlRestClient")
    fun pdlRestClient(
        @Value("\${PDL_SCOPE}") scope: String,
    ): RestClient =
        entraIDRestClientFactory
            .lagHybridRestKlient(scope) { SikkerhetContext.hentJwt()?.tokenValue }
            .medJsonContentType()

    @Bean("pdlRestM2mClient")
    fun pdlRestM2mClient(
        @Value("\${PDL_SCOPE}") scope: String,
    ): RestClient =
        entraIDRestClientFactory
            .lagMaskinTilMaskinRestKlient(scope)
            .medJsonContentType()

    @Bean("økonomiRestClient")
    fun økonomiRestClient(
        @Value("\${FAMILIE_OPPDRAG_SCOPE}") scope: String,
    ): RestClient =
        entraIDRestClientFactory
            .lagHybridRestKlient(scope) { SikkerhetContext.hentJwt()?.tokenValue }
            .medJsonContentType()

    @Bean("infotrygdBarnetrygdRestClient")
    fun infotrygdBarnetrygdRestClient(
        @Value("\${FAMILIE_BA_INFOTRYGD_SCOPE}") scope: String,
    ): RestClient {
        val factory =
            SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(Duration.ofMinutes(12))
                setReadTimeout(Duration.ofMinutes(12))
            }
        return entraIDRestClientFactory
            .lagHybridRestKlient(scope) { SikkerhetContext.hentJwt()?.tokenValue }
            .mutate()
            .requestFactory(factory)
            .build()
            .medJsonContentType()
    }

    @Bean("infotrygdFeedRestClient")
    fun infotrygdFeedRestClient(
        @Value("\${FAMILIE_BA_INFOTRYGD_FEED_SCOPE}") scope: String,
    ): RestClient =
        entraIDRestClientFactory
            .lagMaskinTilMaskinRestKlient(scope)
            .medJsonContentType()

    @Bean("efSakRestClient")
    fun efSakRestClient(
        @Value("\${FAMILIE_EF_SAK_SCOPE}") scope: String,
    ): RestClient =
        entraIDRestClientFactory
            .lagHybridRestKlient(scope) { SikkerhetContext.hentJwt()?.tokenValue }
            .medJsonContentType()

    @Bean("tilbakekrevingRestClient")
    fun tilbakekrevingRestClient(
        @Value("\${FAMILIE_TILBAKE_SCOPE}") scope: String,
    ): RestClient =
        entraIDRestClientFactory
            .lagHybridRestKlient(scope) { SikkerhetContext.hentJwt()?.tokenValue }
            .medJsonContentType()

    @Bean("klageRestClient")
    fun klageRestClient(
        @Value("\${FAMILIE_KLAGE_SCOPE}") scope: String,
    ): RestClient =
        entraIDRestClientFactory
            .lagHybridRestKlient(scope) { SikkerhetContext.hentJwt()?.tokenValue }
            .medJsonContentType()

    @Bean("utenAuthRestClient")
    fun utenAuthRestClient(): RestClient =
        RestClient
            .builder()
            .requestInterceptor(consumerIdClientInterceptor)
            .requestInterceptor(mdcValuesPropagatingClientInterceptor)
            .defaultRequest { it.accept(MediaType.APPLICATION_JSON) }
            .defaultHeaders { it.contentType = MediaType.APPLICATION_JSON }
            .build()

    private fun RestClient.medJsonContentType(): RestClient =
        mutate()
            .defaultHeaders { it.contentType = MediaType.APPLICATION_JSON }
            .build()
}
