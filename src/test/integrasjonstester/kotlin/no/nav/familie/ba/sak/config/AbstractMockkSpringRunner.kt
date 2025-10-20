package no.nav.familie.ba.sak.config

import io.mockk.unmockkAll
import no.nav.familie.ba.sak.fake.FakeEfSakRestClient
import no.nav.familie.ba.sak.fake.FakePdlIdentRestClient
import no.nav.familie.ba.sak.integrasjoner.ef.EfSakRestClient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlIdentRestClient
import no.nav.familie.ba.sak.fake.FakeEfSakRestKlient
import no.nav.familie.ba.sak.fake.FakePdlIdentRestKlient
import no.nav.familie.ba.sak.integrasjoner.ef.EfSakRestKlient
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlIdentRestKlient
import no.nav.familie.ba.sak.mock.FamilieIntegrasjonerTilgangskontrollMock
import org.junit.jupiter.api.BeforeEach
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import java.util.UUID

abstract class AbstractMockkSpringRunner {
    @Autowired
    private lateinit var pdlIdentRestKlient: PdlIdentRestKlient

    @Autowired
    private lateinit var efSakRestKlient: EfSakRestKlient

    @Autowired
    private lateinit var mockFamilieIntegrasjonerTilgangskontrollClient: FamilieIntegrasjonerTilgangskontrollClient

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
        clearMocks()
    }

    private fun clearMocks() {
        unmockkAll()

        val fakePdlIdentRestKlient = pdlIdentRestKlient as? FakePdlIdentRestKlient
        fakePdlIdentRestKlient?.reset()

        val fakeEfSakRestKlient = efSakRestKlient as? FakeEfSakRestKlient
        fakeEfSakRestKlient?.reset()

        FamilieIntegrasjonerTilgangskontrollMock.clearMockFamilieIntegrasjonerTilgangskontrollClient(
            mockFamilieIntegrasjonerTilgangskontrollClient,
        )

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
