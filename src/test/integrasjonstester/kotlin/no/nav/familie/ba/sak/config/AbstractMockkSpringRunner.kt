package no.nav.familie.ba.sak.config

import io.mockk.isMockKMock
import io.mockk.unmockkAll
import no.nav.familie.ba.sak.fake.FakeEfSakRestClient
import no.nav.familie.ba.sak.fake.FakePdlIdentRestClient
import no.nav.familie.ba.sak.integrasjoner.ef.EfSakRestClient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlIdentRestClient
import no.nav.familie.ba.sak.mock.FamilieIntegrasjonerTilgangskontrollMock
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.ba.sak.task.TaskRepositoryTestConfig
import org.junit.jupiter.api.BeforeEach
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import java.util.UUID

abstract class AbstractMockkSpringRunner {
    @Autowired
    private lateinit var pdlIdentRestClient: PdlIdentRestClient

    @Autowired
    private lateinit var efSakRestClient: EfSakRestClient

    @Autowired
    private lateinit var mockFamilieIntegrasjonerTilgangskontrollClient: FamilieIntegrasjonerTilgangskontrollClient

    @Autowired
    private lateinit var mockTaskRepository: TaskRepositoryWrapper

    @Autowired
    private lateinit var mockOpprettTaskService: OpprettTaskService

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

        val fakePdlIdentRestClient = pdlIdentRestClient as? FakePdlIdentRestClient
        fakePdlIdentRestClient?.reset()

        val fakeEfSakRestClient = efSakRestClient as? FakeEfSakRestClient
        fakeEfSakRestClient?.reset()

        FamilieIntegrasjonerTilgangskontrollMock.clearMockFamilieIntegrasjonerTilgangskontrollClient(
            mockFamilieIntegrasjonerTilgangskontrollClient,
        )

        if (isMockKMock(mockTaskRepository)) {
            TaskRepositoryTestConfig.clearMockTaskRepository(mockTaskRepository)
        }

        if (isMockKMock(mockOpprettTaskService)) {
            TaskRepositoryTestConfig.clearMockTaskService(mockOpprettTaskService)
        }

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
