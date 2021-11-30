package no.nav.familie.ba.sak.config

import org.apache.http.HttpException
import org.apache.http.HttpHost
import org.apache.http.HttpRequest
import org.apache.http.client.HttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.conn.DefaultProxyRoutePlanner
import org.apache.http.protocol.HttpContext
import org.slf4j.LoggerFactory
import org.springframework.boot.web.client.RestTemplateCustomizer
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestTemplate

class NaisProxyCustomizer : RestTemplateCustomizer {
    override fun customize(restTemplate: RestTemplate) {
        val proxy = HttpHost("webproxy-nais.nav.no", 8088)
        val client: HttpClient = HttpClientBuilder.create()
            .setRoutePlanner(object : DefaultProxyRoutePlanner(proxy) {
                @Throws(HttpException::class)
                public override fun determineProxy(
                    target: HttpHost,
                    request: HttpRequest,
                    context: HttpContext
                ): HttpHost? {
                    logger.info("Debug: Skal gå mot ${target.hostName} via ${proxy.hostName}")
                    return if (target.hostName.contains("microsoft") ||
                        target.hostName.contains("sanity") ||
                        target.hostName.contains("httpbin")
                    ) {
                        logger.info("Går mot ${target.hostName} via ${proxy.hostName}")
                        super.determineProxy(target, request, context)
                    } else null
                }
            })
            .build()
        restTemplate.requestFactory = HttpComponentsClientHttpRequestFactory(client)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(NaisProxyCustomizer::class.java)
    }
}
