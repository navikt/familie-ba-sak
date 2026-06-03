package no.nav.familie.ba.sak.config

import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.felles.tokenklient.entraid.EntraIDRestClientFactory
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.log.interceptor.ConsumerIdClientInterceptor
import no.nav.familie.log.interceptor.MdcValuesPropagatingClientInterceptor
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.http.converter.ByteArrayHttpMessageConverter
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestOperations
import org.springframework.web.client.RestTemplate
import java.nio.charset.StandardCharsets
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
        entraIDRestClientFactory.lagHybridRestKlient(scope) {
            SikkerhetContext.hentJwt()?.tokenValue
        }

    @Bean("integrasjonerM2mRestClient")
    fun integrasjonerM2mRestClient(
        @Value("\${FAMILIE_INTEGRASJONER_SCOPE}") scope: String,
    ): RestClient = entraIDRestClientFactory.lagMaskinTilMaskinRestKlient(scope)

    @Bean("pdlRestClient")
    fun pdlRestClient(
        @Value("\${PDL_SCOPE}") scope: String,
    ): RestClient =
        entraIDRestClientFactory.lagHybridRestKlient(scope) {
            SikkerhetContext.hentJwt()?.tokenValue
        }

    @Bean("økonomiRestClient")
    fun økonomiRestClient(
        @Value("\${FAMILIE_OPPDRAG_SCOPE}") scope: String,
    ): RestClient =
        entraIDRestClientFactory.lagHybridRestKlient(scope) {
            SikkerhetContext.hentJwt()?.tokenValue
        }

    @Bean("infotrygdBarnetrygdRestClient")
    fun infotrygdBarnetrygdRestClient(
        @Value("\${FAMILIE_BA_INFOTRYGD_SCOPE}") scope: String,
    ): RestClient {
        val base =
            entraIDRestClientFactory.lagHybridRestKlient(scope) {
                SikkerhetContext.hentJwt()?.tokenValue
            }
        val factory =
            SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(Duration.ofMinutes(12))
                setReadTimeout(Duration.ofMinutes(12))
            }
        return base.mutate().requestFactory(factory).build()
    }

    @Bean("infotrygdFeedRestClient")
    fun infotrygdFeedRestClient(
        @Value("\${FAMILIE_BA_INFOTRYGD_FEED_SCOPE}") scope: String,
    ): RestClient = entraIDRestClientFactory.lagMaskinTilMaskinRestKlient(scope)

    @Bean("efSakRestClient")
    fun efSakRestClient(
        @Value("\${FAMILIE_EF_SAK_SCOPE}") scope: String,
    ): RestClient =
        entraIDRestClientFactory.lagHybridRestKlient(scope) {
            SikkerhetContext.hentJwt()?.tokenValue
        }

    @Bean("tilbakekrevingRestClient")
    fun tilbakekrevingRestClient(
        @Value("\${FAMILIE_TILBAKE_SCOPE}") scope: String,
    ): RestClient =
        entraIDRestClientFactory.lagHybridRestKlient(scope) {
            SikkerhetContext.hentJwt()?.tokenValue
        }

    @Bean("klageRestClient")
    fun klageRestClient(
        @Value("\${FAMILIE_KLAGE_SCOPE}") scope: String,
    ): RestClient =
        entraIDRestClientFactory.lagHybridRestKlient(scope) {
            SikkerhetContext.hentJwt()?.tokenValue
        }

    @Bean("utenAuthRestClient")
    fun utenAuthRestClient(): RestClient =
        RestClient
            .builder()
            .requestInterceptor(consumerIdClientInterceptor)
            .requestInterceptor(mdcValuesPropagatingClientInterceptor)
            .defaultRequest { it.accept(MediaType.APPLICATION_JSON) }
            .build()

    @Bean
    fun restOperations(): RestOperations =
        RestTemplate(
            listOf(
                StringHttpMessageConverter(StandardCharsets.UTF_8),
                ByteArrayHttpMessageConverter(),
                JacksonJsonHttpMessageConverter(jsonMapper),
            ),
        ).apply {
            interceptors = listOf(consumerIdClientInterceptor, mdcValuesPropagatingClientInterceptor)
        }
}
