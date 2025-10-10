package no.nav.familie.ba.sak.config

import no.nav.familie.ba.sak.fake.FakeBrevKlient
import no.nav.familie.ba.sak.fake.FakeEfSakRestClient
import no.nav.familie.ba.sak.fake.FakeEnvService
import no.nav.familie.ba.sak.fake.FakeFeatureToggleService
import no.nav.familie.ba.sak.fake.FakeIntegrasjonClient
import no.nav.familie.ba.sak.fake.FakeLeaderClientService
import no.nav.familie.ba.sak.fake.FakePdlIdentRestClient
import no.nav.familie.ba.sak.fake.FakePdlRestClient
import no.nav.familie.ba.sak.fake.FakePersonopplysningerService
import no.nav.familie.ba.sak.fake.FakeSanityKlient
import no.nav.familie.ba.sak.fake.FakeSystemOnlyIntegrasjonClient
import no.nav.familie.ba.sak.fake.FakeTaskRepositoryWrapper
import no.nav.familie.ba.sak.fake.FakeTilbakekrevingKlient
import no.nav.familie.ba.sak.fake.FakeValutakursRestClient
import no.nav.familie.ba.sak.fake.FakeØkonomiKlient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollService
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestClient
import no.nav.familie.ba.sak.internal.TestVerktøyService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.unleash.UnleashService
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.web.client.RestOperations

@TestConfiguration
class FakeConfig {
    @Bean
    @Primary
    @Profile("fake-integrasjon-client")
    fun fakeIntegrasjonClient(restOperations: RestOperations): FakeIntegrasjonClient = FakeIntegrasjonClient(restOperations)

    @Bean
    @Primary
    @Profile("fake-valutakurs-rest-client")
    fun fakeValutakursRestClient(restOperations: RestOperations): FakeValutakursRestClient = FakeValutakursRestClient(restOperations)

    @Bean
    @Primary
    @Profile("fake-økonomi-klient")
    fun fakeØkonomiKlient(restOperations: RestOperations): FakeØkonomiKlient = FakeØkonomiKlient(restOperations)

    @Bean
    @Primary
    @Profile("fake-tilbakekreving-klient")
    fun fakeTilbakekrevingKlient(restOperations: RestOperations): FakeTilbakekrevingKlient = FakeTilbakekrevingKlient(restOperations)

    @Bean
    @Primary
    @Profile("mock-unleash")
    fun fakeFeatureToggleService(
        unleashService: UnleashService,
        behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
        arbeidsfordelingPåBehandlingRepository: ArbeidsfordelingPåBehandlingRepository,
    ): FakeFeatureToggleService =
        FakeFeatureToggleService(
            unleashService = unleashService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            arbeidsfordelingPåBehandlingRepository = arbeidsfordelingPåBehandlingRepository,
        )

    @Bean
    @Primary
    @Profile("mock-brev-klient")
    fun fakeBrevKlient(testVerktøyService: TestVerktøyService): FakeBrevKlient = FakeBrevKlient(testVerktøyService)

    @Bean
    @Primary
    @Profile("mock-leader-client")
    fun fakeLeaderClientService(): FakeLeaderClientService = FakeLeaderClientService()

    @Bean
    @Primary
    @Profile("mock-pdl-client")
    fun fakePdlRestClient(
        restOperations: RestOperations,
        personidentService: PersonidentService,
    ): FakePdlRestClient =
        FakePdlRestClient(
            restOperations = restOperations,
            personidentService = personidentService,
        )

    @Bean
    @Primary
    @Profile("mock-system-only-integrasjon-client")
    fun fakeSystemOnlyIntegrasjonClient(): FakeSystemOnlyIntegrasjonClient = FakeSystemOnlyIntegrasjonClient()

    @Bean
    @Primary
    @Profile("mock-ef-client")
    fun fakeEfSakRestClient(restOperations: RestOperations): FakeEfSakRestClient = FakeEfSakRestClient(restOperations)

    @Bean
    @Primary
    @Profile("fake-env-service")
    fun fakeEnvService(environment: Environment): FakeEnvService = FakeEnvService(environment)

    @Bean
    @Primary
    @Profile("mock-ident-client")
    fun fakePdlIdentRestClient(restOperations: RestOperations): FakePdlIdentRestClient = FakePdlIdentRestClient(restOperations)

    @Bean
    @Primary
    @Profile("mock-pdl")
    fun fakePersonopplysningerService(
        pdlRestClient: PdlRestClient,
        systemOnlyPdlRestClient: SystemOnlyPdlRestClient,
        familieIntegrasjonerTilgangskontrollService: FamilieIntegrasjonerTilgangskontrollService,
        integrasjonClient: IntegrasjonClient,
    ): FakePersonopplysningerService =
        FakePersonopplysningerService(
            pdlRestClient = pdlRestClient,
            systemOnlyPdlRestClient = systemOnlyPdlRestClient,
            familieIntegrasjonerTilgangskontrollService = familieIntegrasjonerTilgangskontrollService,
            integrasjonClient = integrasjonClient,
        )

    @Bean
    @Primary
    @Profile("mock-sanity-client")
    fun fakeSanityKlient() = FakeSanityKlient()

    @Bean
    @Primary
    @Profile("fake-task-repository")
    fun fakeTaskRepositoryWrapper(taskService: TaskService): FakeTaskRepositoryWrapper = FakeTaskRepositoryWrapper(taskService)
}
