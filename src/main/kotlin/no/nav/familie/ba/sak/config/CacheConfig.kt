package no.nav.familie.ba.sak.config

import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCache
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import java.util.concurrent.TimeUnit

@Configuration
@EnableCaching
class CacheConfig {
    @Bean
    @Primary
    fun cacheManager(): CacheManager =
        object : ConcurrentMapCacheManager() {
            override fun createConcurrentMapCache(name: String): Cache {
                val concurrentMap =
                    Caffeine
                        .newBuilder()
                        .maximumSize(1000)
                        .expireAfterWrite(60, TimeUnit.MINUTES)
                        .recordStats()
                        .build<Any, Any>()
                        .asMap()
                return ConcurrentMapCache(name, concurrentMap, true)
            }
        }

    @Bean("shortCache")
    fun shortCache(): CacheManager =
        object : ConcurrentMapCacheManager() {
            override fun createConcurrentMapCache(name: String): Cache {
                val concurrentMap =
                    Caffeine
                        .newBuilder()
                        .maximumSize(1000)
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .recordStats()
                        .build<Any, Any>()
                        .asMap()
                return ConcurrentMapCache(name, concurrentMap, true)
            }
        }

    @Bean("dailyCache")
    fun dailyCache(): CacheManager =
        object : ConcurrentMapCacheManager() {
            override fun createConcurrentMapCache(name: String): Cache {
                val concurrentMap =
                    Caffeine
                        .newBuilder()
                        .maximumSize(1000)
                        .expireAfterWrite(24, TimeUnit.HOURS)
                        .recordStats()
                        .build<Any, Any>()
                        .asMap()
                return ConcurrentMapCache(name, concurrentMap, true)
            }
        }
}

fun CacheManager.getCacheOrThrow(cache: String) = this.getCache(cache) ?: error("Finner ikke cache=$cache")

/**
 * Henter tidligere cachet verdier, og henter ucachet verdier med [valueLoader]
 * Caches per saksbehandler, sånn at man eks kan hente tilgang for gitt saksbehandler
 */
fun <T, RESULT> CacheManager.hentCacheForSaksbehandler(
    cacheName: String,
    verdi: T,
    hentVerdi: (T) -> RESULT,
): RESULT {
    val cache = this.getCacheOrThrow(cacheName)
    return cache.get(Pair(SikkerhetContext.hentSaksbehandler(), verdi)) { hentVerdi(verdi) } ?: throw Feil("Finner ikke verdi fra cache=$cacheName")
}
