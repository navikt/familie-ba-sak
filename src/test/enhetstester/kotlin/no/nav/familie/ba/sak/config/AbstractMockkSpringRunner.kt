package no.nav.familie.ba.sak.config

import io.mockk.isMockKMock
import io.mockk.unmockkAll
import io.sentry.spring.boot.SentryAutoConfiguration
import no.nav.familie.ba.sak.common.LocalDateService
import no.nav.familie.ba.sak.integrasjoner.`ef-sak`.EfSakRestClient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdBarnetrygdClient
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdBarnetrygdClientMock
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlIdentRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiKlient
import no.nav.familie.ba.sak.kjerne.tilbakekreving.TilbakekrevingKlient
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.ba.sak.task.TaskRepositoryTestConfig
import org.junit.jupiter.api.BeforeEach
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.actuate.autoconfigure.metrics.JvmMetricsAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.metrics.LogbackMetricsAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsEndpointAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.metrics.SystemMetricsAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.metrics.data.RepositoryMetricsAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.metrics.startup.StartupTimeMetricsListenerAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.metrics.web.jetty.JettyMetricsAutoConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.cache.CacheManager
import org.springframework.context.ConfigurableApplicationContext

@EnableAutoConfiguration(
    exclude = arrayOf(
        JettyMetricsAutoConfiguration::class,
        JvmMetricsAutoConfiguration::class,
        LogbackMetricsAutoConfiguration::class,
        MetricsAutoConfiguration::class,
        MetricsEndpointAutoConfiguration::class,
        PrometheusMetricsExportAutoConfiguration::class,
        RepositoryMetricsAutoConfiguration::class,
        SentryAutoConfiguration::class,
        StartupTimeMetricsListenerAutoConfiguration::class,
        SystemMetricsAutoConfiguration::class
    )
)
abstract class AbstractMockkSpringRunner {
    /**
     * Tjenester vi mocker ved bruk av every
     */
    @Autowired
    private lateinit var mockPersonopplysningerService: PersonopplysningerService

    @Autowired
    private lateinit var mockPdlIdentRestClient: PdlIdentRestClient

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

    @Autowired
    private lateinit var mockInfotrygdBarnetrygdClient: InfotrygdBarnetrygdClient

    @Autowired
    private lateinit var mockTaskRepository: TaskRepositoryWrapper

    @Autowired
    private lateinit var mockOpprettTaskService: OpprettTaskService

    @Autowired
    private lateinit var applicationContext: ConfigurableApplicationContext

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

    @BeforeEach
    fun reset() {
        clearCaches()
        clearMocks()
    }

    private fun clearMocks() {
        unmockkAll()
        if (isMockKMock(mockPersonopplysningerService)) {
            ClientMocks.clearPdlMocks(mockPersonopplysningerService)
        }

        if (isMockKMock(mockPdlIdentRestClient)) {
            ClientMocks.clearPdlIdentRestClient(mockPdlIdentRestClient)
        }

        IntegrasjonClientMock.clearIntegrasjonMocks(mockIntegrasjonClient)

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
            TilbakekrevingKlientTestConfig.clearTilbakekrevingKlientMocks(mockTilbakekrevingKlient)
        }

        if (isMockKMock(mockLocalDateService)) {
            LocalDateServiceTestConfig.clearLocalDateServiceMocks(mockLocalDateService)
        }

        if (isMockKMock(mockInfotrygdBarnetrygdClient)) {
            InfotrygdBarnetrygdClientMock.clearInfotrygdBarnetrygdMocks(mockInfotrygdBarnetrygdClient)
        }

        if (isMockKMock(mockTaskRepository)) {
            TaskRepositoryTestConfig.clearMockTaskRepository(mockTaskRepository)
        }

        if (isMockKMock(mockOpprettTaskService)) {
            TaskRepositoryTestConfig.clearMockTaskService(mockOpprettTaskService)
        }

        MDC.put("callId", "callId")
    }

    private fun clearCaches() {
        listOf(defaultCacheManager, shortCacheManager, kodeverkCacheManager).forEach {
            it.cacheNames.mapNotNull { cacheName -> it.getCache(cacheName) }
                .forEach { cache -> cache.clear() }
        }
    }
}
