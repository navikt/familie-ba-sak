package no.nav.familie.ba.sak.config

import org.junit.jupiter.api.BeforeEach
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
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
}
