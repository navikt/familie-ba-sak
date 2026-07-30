package no.nav.familie.ba.sak.config

import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.BeforeEach
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.util.UUID

abstract class AbstractMockkSpringRunner {
    /**
     * Cachemanagere
     */
    @Autowired
    private lateinit var defaultCacheManager: CacheManager

    @Autowired
    @Qualifier("dailyCache")
    private lateinit var dailyCacheManager: CacheManager

    @Autowired
    @Qualifier("shortCache")
    private lateinit var shortCacheManager: CacheManager

    @BeforeEach
    fun reset() {
        clearCaches()
        setCallIdForLogging()
    }

    private fun setCallIdForLogging() {
        MDC.put("callId", "${this::class.java.simpleName}-${UUID.randomUUID()}")
    }

    private fun clearCaches() {
        listOf(defaultCacheManager, shortCacheManager, dailyCacheManager).forEach {
            it.cacheNames
                .mapNotNull { cacheName -> it.getCache(cacheName) }
                .forEach { cache -> cache.clear() }
        }
    }

    companion object {
        val mockOAuth2Server =
            MockOAuth2Server().also {
                it.start()
                Runtime.getRuntime().addShutdownHook(Thread { it.shutdown() })
            }

        @JvmStatic
        @DynamicPropertySource
        @Suppress("unused")
        fun registrerMockOAuth2ServerProperties(registry: DynamicPropertyRegistry) {
            val port = mockOAuth2Server.config.httpServer.port()
            registry.add("AZURE_OPENID_CONFIG_ISSUER") { "http://localhost:$port/azuread" }
            registry.add("AZURE_OPENID_CONFIG_JWKS_URI") { "http://localhost:$port/azuread/jwks" }
            registry.add("AZURE_APP_CLIENT_ID") { "some-audience" }
            registry.add("TOKEN_X_ISSUER") { "http://localhost:$port/tokenx" }
            registry.add("TOKEN_X_JWKS_URI") { "http://localhost:$port/tokenx/jwks" }
            registry.add("TOKEN_X_CLIENT_ID") { "some-audience" }
        }
    }
}
