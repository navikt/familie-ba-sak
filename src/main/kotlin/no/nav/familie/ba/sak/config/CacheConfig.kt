package no.nav.familie.ba.sak.config

import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCache
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.cache.RedisCacheManager.RedisCacheManagerBuilder
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
import org.springframework.data.redis.serializer.RedisSerializer
import java.time.Duration
import java.util.concurrent.TimeUnit

@Configuration
@EnableCaching
class CacheConfig {

    @Bean
    @Primary
    fun cacheManager(): CacheManager = object : ConcurrentMapCacheManager() {
        override fun createConcurrentMapCache(name: String): Cache {
            val concurrentMap = Caffeine
                .newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(60, TimeUnit.MINUTES)
                .recordStats().build<Any, Any>().asMap()
            return ConcurrentMapCache(name, concurrentMap, true)
        }
    }

    @Bean("shortCache")
    fun shortCache(): CacheManager = object : ConcurrentMapCacheManager() {
        override fun createConcurrentMapCache(name: String): Cache {
            val concurrentMap = Caffeine
                .newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats().build<Any, Any>().asMap()
            return ConcurrentMapCache(name, concurrentMap, true)
        }
    }

    @Bean("dailyCache")
    fun dailyCache(): CacheManager = object : ConcurrentMapCacheManager() {
        override fun createConcurrentMapCache(name: String): Cache {
            val concurrentMap = Caffeine
                .newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(24, TimeUnit.HOURS)
                .recordStats().build<Any, Any>().asMap()
            return ConcurrentMapCache(name, concurrentMap, true)
        }
    }

    @Bean(value = ["redisCacheManager"])
    fun redisCacheManager(lettuceConnectionFactory: LettuceConnectionFactory?): CacheManager {
        val redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
            .disableCachingNullValues()
            .entryTtl(Duration.ofMinutes(60))
            .serializeValuesWith(SerializationPair.fromSerializer<Any>(RedisSerializer.json()))
        redisCacheConfiguration.usePrefix()
        return RedisCacheManagerBuilder.fromConnectionFactory(lettuceConnectionFactory!!)
            .cacheDefaults(redisCacheConfiguration).build()
    }

    @Bean("redisCacheI90Dager")
    fun cacheConfiguration(connectionFactory: RedisConnectionFactory): CacheManager {
        val rediscacheconfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofDays(90))
            .disableCachingNullValues()
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(GenericJackson2JsonRedisSerializer()))
        val cm: RedisCacheManager = RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(rediscacheconfig)
            .transactionAware()
            .build()
        return cm
    }

    @Bean("skattPersonerCache")
    fun skattPersonerCache(): CacheManager = object : ConcurrentMapCacheManager() {
        override fun createConcurrentMapCache(name: String): Cache {
            val concurrentMap = Caffeine
                .newBuilder()
                .initialCapacity(100)
                .maximumSize(1000)
                .expireAfterWrite(1, TimeUnit.DAYS)
                .recordStats().build<Any, Any>().asMap()
            return ConcurrentMapCache(name, concurrentMap, true)
        }
    }
}

fun CacheManager.getCacheOrThrow(cache: String) = this.getCache(cache) ?: error("Finner ikke cache=$cache")

/**
 * Henter tidligere cachet verdier, og henter ucachet verdier med [valueLoader]
 * Caches per saksbehandler, sånn at man eks kan hente tilgang for gitt saksbehandler
 */
@Suppress("UNCHECKED_CAST", "NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
fun <VALUE : Any, RESULT> CacheManager.hentCacheForSaksbehandler(
    cacheName: String,
    values: List<VALUE>,
    valueLoader: (List<VALUE>) -> Map<VALUE, RESULT>,
): Map<VALUE, RESULT> {
    val cache = this.getCacheOrThrow(cacheName)
    val saksbehandler = SikkerhetContext.hentSaksbehandler()

    val previousValues: List<Pair<VALUE, RESULT?>> = values.distinct()
        .map { it to cache.get(Pair(saksbehandler, it))?.get() as RESULT? }

    val cachedValues = previousValues.mapNotNull { if (it.second == null) null else it }.toMap() as Map<VALUE, RESULT>
    val valuesWithoutCache = previousValues.filter { it.second == null }.map { it.first }
    val loadedValues: Map<VALUE, RESULT> = valuesWithoutCache
        .takeIf { it.isNotEmpty() }
        ?.let { valueLoader(it) } ?: emptyMap()
    loadedValues.forEach { cache.put(Pair(saksbehandler, it.key), it.value) }

    return cachedValues + loadedValues
}
