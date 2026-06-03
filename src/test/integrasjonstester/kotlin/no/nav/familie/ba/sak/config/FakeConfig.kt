package no.nav.familie.ba.sak.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.fake.FakeBrevKlient
import no.nav.familie.ba.sak.fake.FakeECBValutakursRestKlient
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
import no.nav.familie.ba.sak.fake.FakeØkonomiKlient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollService
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestKlient
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestKlient
import no.nav.familie.ba.sak.internal.TestVerktøyService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.falskidentitet.FalskIdentitetService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.felles.tokenklient.entraid.EntraIDClient
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.unleash.UnleashService
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment

@TestConfiguration
class FakeConfig {
    @Bean
    @Primary
    fun entraIDClientMock(): EntraIDClient {
        val mock = mockk<EntraIDClient>(relaxed = true)
        every { mock.hentMaskinTilMaskinToken(any()) } returns "mock-m2m-token"
        every { mock.hentOboToken(any(), any()) } returns "mock-obo-token"
        return mock
    }

    @Bean
    @Primary
    @Profile("fake-integrasjon-klient")
    fun fakeIntegrasjonKlient(): FakeIntegrasjonKlient = FakeIntegrasjonKlient()

    @Bean
    @Primary
    @Profile("fake-ecb-valutakurs-rest-klient")
    fun fakeECBValutakursRestKlient(): FakeECBValutakursRestKlient = FakeECBValutakursRestKlient()

    @Bean
    @Primary
    @Profile("fake-økonomi-klient")
    fun fakeØkonomiKlient(): FakeØkonomiKlient = FakeØkonomiKlient()

    @Bean
    @Primary
    @Profile("fake-tilbakekreving-klient")
    fun fakeTilbakekrevingKlient(): FakeTilbakekrevingKlient = FakeTilbakekrevingKlient()

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
        personidentService: PersonidentService,
    ): FakePdlRestKlient =
        FakePdlRestKlient(
            personidentService = personidentService,
        )

    @Bean
    @Primary
    @Profile("mock-system-only-integrasjon-klient")
    fun fakeSystemOnlyIntegrasjonKlient(): FakeSystemOnlyIntegrasjonKlient = FakeSystemOnlyIntegrasjonKlient()

    @Bean
    @Primary
    @Profile("mock-ef-klient")
    fun fakeEfSakRestKlient(): FakeEfSakRestKlient = FakeEfSakRestKlient()

    @Bean
    @Primary
    @Profile("fake-env-service")
    fun fakeEnvService(environment: Environment): FakeEnvService = FakeEnvService(environment)

    @Bean
    @Primary
    @Profile("mock-ident-klient")
    fun fakePdlIdentRestKlient(): FakePdlIdentRestKlient = FakePdlIdentRestKlient()

    @Bean
    @Primary
    @Profile("mock-pdl")
    fun fakePersonopplysningerService(
        pdlRestKlient: PdlRestKlient,
        systemOnlyPdlRestKlient: SystemOnlyPdlRestKlient,
        familieIntegrasjonerTilgangskontrollService: FamilieIntegrasjonerTilgangskontrollService,
        integrasjonKlient: IntegrasjonKlient,
        falskIdentitetService: FalskIdentitetService,
    ): FakePersonopplysningerService =
        FakePersonopplysningerService(
            pdlRestKlient = pdlRestKlient,
            systemOnlyPdlRestKlient = systemOnlyPdlRestKlient,
            familieIntegrasjonerTilgangskontrollService = familieIntegrasjonerTilgangskontrollService,
            integrasjonKlient = integrasjonKlient,
            falskIdentitetService = falskIdentitetService,
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
    fun fakeInfotrygdBarnetrygdKlient(): FakeInfotrygdBarnetrygdKlient = FakeInfotrygdBarnetrygdKlient()
}
