package no.nav.familie.ba.sak.behandling.fødselshendelse

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ba.sak.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.fødselshendelse.MockConfiguration.Companion.barnefnr
import no.nav.familie.ba.sak.behandling.fødselshendelse.MockConfiguration.Companion.morsfnr
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.*
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatRepository
import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.infotrygd.InfotrygdBarnetrygdClient
import no.nav.familie.ba.sak.infotrygd.InfotrygdFeedService
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.pdl.internal.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.ba.sak.pdl.internal.IdentInformasjon
import no.nav.familie.ba.sak.pdl.internal.PersonInfo
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.kontrakter.felles.personopplysning.*
import no.nav.familie.prosessering.domene.TaskRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("dev", "mock-dokgen", "mock-oauth", "mock-pdl-flere-barn")
@Tag("integration")
class FødselshendelseIntegrasjonTest(
        @Autowired
        private val stegService: StegService,

        @Autowired
        private val behandlingRepository: BehandlingRepository,

        @Autowired
        private val behandlingResultatRepository: BehandlingResultatRepository,

        @Autowired
        private val taskRepository: TaskRepository,

        @Autowired
        private val evaluerFiltreringsreglerForFødselshendelse: EvaluerFiltreringsreglerForFødselshendelse,

        @Autowired
        private val vedtakService: VedtakService,

        @Autowired
        private val persongrunnlagService: PersongrunnlagService,

        @Autowired
        private val personopplysningerService: PersonopplysningerService
) {

    val infotrygdBarnetrygdClientMock = mockk<InfotrygdBarnetrygdClient>()
    val infotrygdFeedServiceMock = mockk<InfotrygdFeedService>()
    val featureToggleServiceMock = mockk<FeatureToggleService>()


    val fødselshendelseService = FødselshendelseService(infotrygdFeedServiceMock,
                                                        infotrygdBarnetrygdClientMock,
                                                        featureToggleServiceMock,
                                                        stegService,
                                                        vedtakService,
                                                        evaluerFiltreringsreglerForFødselshendelse,
                                                        taskRepository,
                                                        personopplysningerService,
                                                        behandlingResultatRepository,
                                                        persongrunnlagService,
                                                        behandlingRepository)

    @Test
    fun `Fødselshendelse med flere barn skal bli håndtert riktige`() {
        fødselshendelseService.opprettBehandlingOgKjørReglerForFødselshendelse(NyBehandlingHendelse(
                morsfnr, morsfnr, barnefnr
        ))
    }

    @BeforeEach
    fun initMocks() {
        every { infotrygdFeedServiceMock.sendTilInfotrygdFeed(any()) } returns Unit
        every { featureToggleServiceMock.isEnabled(any()) } returns false
    }
}

@Configuration
class MockConfiguration{
    @Bean
    @Profile("mock-pdl-flere-barn")
    @Primary
    fun mockPersonopplysningsService(): PersonopplysningerService {
        val personopplysningerServiceMock = mockk<PersonopplysningerService>()

        val identSlot = slot<Ident>()

        every {
            personopplysningerServiceMock.hentIdenter(capture(identSlot))
        } answers {
            listOf(IdentInformasjon(identSlot.captured.ident, false, "FOLKEREGISTERIDENT"))
        }

        every {
            personopplysningerServiceMock.hentPersoninfoFor(morsfnr)
        } returns PersonInfo(
                fødselsdato = LocalDate.now().minusYears(20),
                navn = "Mor Søker",
                kjønn = Kjønn.KVINNE,
                sivilstand = SIVILSTAND.UGIFT,
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresse = søkerBostedsadresse
        )

        every {
            personopplysningerServiceMock.hentPersoninfoFor(barnefnr[0])
        } returns PersonInfo(
                fødselsdato = LocalDate.now().minusMonths(5),
                navn = "Gutt Barn",
                kjønn = Kjønn.MANN,
                sivilstand = SIVILSTAND.UGIFT,
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresse = søkerBostedsadresse
        )

        every {
            personopplysningerServiceMock.hentPersoninfoFor(barnefnr[1])
        } returns PersonInfo(
                fødselsdato = LocalDate.now().minusMonths(5),
                navn = "Jente Barn",
                kjønn = Kjønn.KVINNE,
                sivilstand = SIVILSTAND.UGIFT,
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresse = søkerBostedsadresse
        )

        val hentAktørIdIdentSlot = slot<Ident>()
        every{
            personopplysningerServiceMock.hentAktivAktørId(capture(hentAktørIdIdentSlot))
        } answers {
            AktørId(id = "0${hentAktørIdIdentSlot.captured.ident}")
        }

        every{
            personopplysningerServiceMock.hentStatsborgerskap(any())
        } returns listOf(Statsborgerskap(land = "NOR", gyldigFraOgMed = LocalDate.now().minusYears(20), gyldigTilOgMed = null))
        return personopplysningerServiceMock

        every{
            personopplysningerServiceMock.hentOpphold(any())
        }returns listOf(Opphold(OPPHOLDSTILLATELSE.PERMANENT, null, null))
    }

    companion object {
        val morsfnr = "12345678910"
        val barnefnr = listOf("12345678911", "12345678912")

        val søkerBostedsadresse = Bostedsadresse(
                vegadresse = Vegadresse(matrikkelId = 1111, husnummer = null, husbokstav = null,
                                        bruksenhetsnummer = null, adressenavn = null, kommunenummer = null,
                                        tilleggsnavn = null, postnummer = "2222")
        )
    }

}
