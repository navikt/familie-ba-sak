package no.nav.familie.ba.sak.config

import io.mockk.isMockKMock
import io.mockk.unmockkAll
import no.nav.familie.ba.sak.common.LocalDateService
import no.nav.familie.ba.sak.integrasjoner.`ef-sak`.EfSakRestClient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiKlient
import no.nav.familie.ba.sak.kjerne.tilbakekreving.TilbakekrevingKlient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractMockkSpringRunner {
    /**
     * Tjenester vi mocker ved bruk av every
     */
    @Autowired
    private lateinit var mockPersonopplysningerService: PersonopplysningerService

    @Autowired
    private lateinit var mockIntegrasjonClient: IntegrasjonClient

    @Autowired
    private lateinit var mockEfSakRestClient: EfSakRestClient

    @Autowired
    private lateinit var mockØkonomiKlient: ØkonomiKlient

    @Autowired
    private lateinit var mockFeatureToggleService: FeatureToggleService

    @Autowired
    private lateinit var mockTilbakekrevingKlient: TilbakekrevingKlient

    @Autowired
    private lateinit var mockLocalDateService: LocalDateService

    /**
     * Cachemanagere
     */
    @Autowired
    private lateinit var defaultCacheManager: CacheManager

    @Autowired
    @Qualifier("kodeverkCache")
    private lateinit var kodeverkCacheManager: CacheManager

    @Autowired
    @Qualifier("shortCache")
    private lateinit var shortCacheManager: CacheManager

    @AfterEach
    fun reset() {
        clearCaches()
        clearMocks()
    }

    private fun clearMocks() {
        unmockkAll()
        if (isMockKMock(mockPersonopplysningerService)) {
            ClientMocks.clearPdlMocks(mockPersonopplysningerService)
        }

        if (isMockKMock(mockIntegrasjonClient)) {
            ClientMocks.clearIntegrasjonMocks(mockIntegrasjonClient)
        }

        if (isMockKMock(mockFeatureToggleService)) {
            ClientMocks.clearFeatureToggleMocks(mockFeatureToggleService)
        }

        if (isMockKMock(mockEfSakRestClient)) {
            EfSakRestClientMock.clearEfSakRestMocks(mockEfSakRestClient)
        }

        if (isMockKMock(mockØkonomiKlient)) {
            ØkonomiTestConfig.clearØkonomiMocks(mockØkonomiKlient)
        }

        if (isMockKMock(mockTilbakekrevingKlient)) {
            TilbakekrevingKlientTestConfig.clearMocks(mockTilbakekrevingKlient)
        }

        if (isMockKMock(mockLocalDateService)) {
            LocalDateServiceTestConfig.clearMocks(mockLocalDateService)
        }
    }

    private fun clearCaches() {
        listOf(defaultCacheManager, shortCacheManager, kodeverkCacheManager).forEach {
            it.cacheNames.mapNotNull { cacheName -> it.getCache(cacheName) }
                .forEach { cache -> cache.clear() }
        }
    }
}
