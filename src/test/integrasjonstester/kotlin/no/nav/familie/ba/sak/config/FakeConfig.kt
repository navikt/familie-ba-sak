package no.nav.familie.ba.sak.config

import no.nav.familie.ba.sak.fake.FakeBrevKlient
import no.nav.familie.ba.sak.fake.FakeEfSakRestKlient
import no.nav.familie.ba.sak.fake.FakeEnvService
import no.nav.familie.ba.sak.fake.FakeFeatureToggleService
import no.nav.familie.ba.sak.fake.FakeInfotrygdBarnetrygdKlient
import no.nav.familie.ba.sak.fake.FakeIntegrasjonKlient
import no.nav.familie.ba.sak.fake.FakeLeaderClientService
import no.nav.familie.ba.sak.fake.FakePdlIdentRestKlient
import no.nav.familie.ba.sak.fake.FakePdlRestKlient
import no.nav.familie.ba.sak.fake.FakePersonopplysningerService
import no.nav.familie.ba.sak.fake.FakeSanityKlient
import no.nav.familie.ba.sak.fake.FakeSystemOnlyIntegrasjonKlient
import no.nav.familie.ba.sak.fake.FakeTaskRepositoryWrapper
import no.nav.familie.ba.sak.fake.FakeTilbakekrevingKlient
import no.nav.familie.ba.sak.fake.FakeValutakursRestClient
import no.nav.familie.ba.sak.fake.FakeØkonomiKlient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollService
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestKlient
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestKlient
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
    @Profile("fake-integrasjon-klient")
    fun fakeIntegrasjonKlient(restOperations: RestOperations): FakeIntegrasjonKlient = FakeIntegrasjonKlient(restOperations)

    @Bean
    @Primary
    @Profile("fake-valutakurs-rest-klient")
    fun fakeValutakursRestKlient(restOperations: RestOperations): FakeValutakursRestClient = FakeValutakursRestClient(restOperations)

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
    @Profile("mock-pdl-klient")
    fun fakePdlRestKlient(
        restOperations: RestOperations,
        personidentService: PersonidentService,
    ): FakePdlRestKlient =
        FakePdlRestKlient(
            restOperations = restOperations,
            personidentService = personidentService,
        )

    @Bean
    @Primary
    @Profile("mock-system-only-integrasjon-klient")
    fun fakeSystemOnlyIntegrasjonKlient(): FakeSystemOnlyIntegrasjonKlient = FakeSystemOnlyIntegrasjonKlient()

    @Bean
    @Primary
    @Profile("mock-ef-klient")
    fun fakeEfSakRestKlient(restOperations: RestOperations): FakeEfSakRestKlient = FakeEfSakRestKlient(restOperations)

    @Bean
    @Primary
    @Profile("fake-env-service")
    fun fakeEnvService(environment: Environment): FakeEnvService = FakeEnvService(environment)

    @Bean
    @Primary
    @Profile("mock-ident-klient")
    fun fakePdlIdentRestKlient(restOperations: RestOperations): FakePdlIdentRestKlient = FakePdlIdentRestKlient(restOperations)

    @Bean
    @Primary
    @Profile("mock-pdl")
    fun fakePersonopplysningerService(
        pdlRestKlient: PdlRestKlient,
        systemOnlyPdlRestKlient: SystemOnlyPdlRestKlient,
        familieIntegrasjonerTilgangskontrollService: FamilieIntegrasjonerTilgangskontrollService,
        integrasjonKlient: IntegrasjonKlient,
    ): FakePersonopplysningerService =
        FakePersonopplysningerService(
            pdlRestKlient = pdlRestKlient,
            systemOnlyPdlRestKlient = systemOnlyPdlRestKlient,
            familieIntegrasjonerTilgangskontrollService = familieIntegrasjonerTilgangskontrollService,
            integrasjonKlient = integrasjonKlient,
        )

    @Bean
    @Primary
    @Profile("mock-sanity-klient")
    fun fakeSanityKlient() = FakeSanityKlient()

    @Bean
    @Primary
    @Profile("fake-task-repository")
    fun fakeTaskRepositoryWrapper(taskService: TaskService): FakeTaskRepositoryWrapper = FakeTaskRepositoryWrapper(taskService)

    @Bean
    @Primary
    @Profile("mock-infotrygd-barnetrygd")
    fun fakeInfotrygdBarnetrygdKlient(restOperations: RestOperations): FakeInfotrygdBarnetrygdKlient = FakeInfotrygdBarnetrygdKlient(restOperations)
}
