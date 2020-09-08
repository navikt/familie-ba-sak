package no.nav.familie.ba.sak.behandling.fødselshendelse

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ba.sak.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.fagsak.FagsakRepository
import no.nav.familie.ba.sak.behandling.fødselshendelse.MockConfiguration.Companion.barnefnr
import no.nav.familie.ba.sak.behandling.fødselshendelse.MockConfiguration.Companion.morsfnr
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.*
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatRepository
import no.nav.familie.ba.sak.beregning.SatsService
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.beregning.domene.SatsType
import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.infotrygd.InfotrygdBarnetrygdClient
import no.nav.familie.ba.sak.infotrygd.InfotrygdFeedService
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.pdl.internal.*
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakter.felles.personopplysning.*
import no.nav.familie.prosessering.domene.TaskRepository
import org.junit.Assert
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
        private val personopplysningerService: PersonopplysningerService,

        @Autowired
        private val fagsakRepository: FagsakRepository,

        @Autowired
        private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository
) {

    val now = LocalDate.now()
    
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
        val fagsak = fagsakRepository.finnFagsakForPersonIdent(PersonIdent(morsfnr))
        val behandling = behandlingRepository.findByFagsakAndAktiv(fagsak!!.id)
        val behandlingResultater = behandlingResultatRepository.finnBehandlingResultater(behandling!!.id)

        Assert.assertEquals(1, behandlingResultater.size)
        Assert.assertEquals(true, behandlingResultater.get(0).aktiv)

        Assert.assertEquals(3, behandlingResultater.get(0).personResultater.size)
        behandlingResultater.forEach {
            it.personResultater.forEach { result -> barnefnr.plus(morsfnr).contains(result.personIdent) }
            it.personResultater.forEach { result -> result.vilkårResultater.forEach { vilkårResultat -> Assert.assertTrue(vilkårResultat.resultat.name == "JA") }}
        }

        val andelTilkjentYtelser = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlinger(listOf(behandling.id))

        val sats = SatsService.hentGyldigSatsFor(SatsType.ORBA, now)


        andelTilkjentYtelser.get(0).beløp
        andelTilkjentYtelser.get(0).stønadFom
        andelTilkjentYtelser.get(0).stønadTom
    }

    @BeforeEach
    fun initMocks() {
        every { infotrygdFeedServiceMock.sendTilInfotrygdFeed(any()) } returns Unit
        every { featureToggleServiceMock.isEnabled(any()) } returns false
    }
}

@Configuration
class MockConfiguration {
    val now = LocalDate.now()
    
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
                fødselsdato = now.minusYears(20),
                navn = "Mor Søker",
                kjønn = Kjønn.KVINNE,
                sivilstand = SIVILSTAND.UGIFT,
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresse = søkerBostedsadresse
        )

        every {
            personopplysningerServiceMock.hentPersoninfoFor(barnefnr[0])
        } returns PersonInfo(
                fødselsdato = now.minusMonths(5),
                navn = "Gutt Barn",
                kjønn = Kjønn.MANN,
                sivilstand = SIVILSTAND.UGIFT,
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresse = søkerBostedsadresse
        )

        every {
            personopplysningerServiceMock.hentPersoninfoFor(barnefnr[1])
        } returns PersonInfo(
                fødselsdato = now.minusMonths(5),
                navn = "Jente Barn",
                kjønn = Kjønn.KVINNE,
                sivilstand = SIVILSTAND.UGIFT,
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresse = søkerBostedsadresse
        )

        val hentAktørIdIdentSlot = slot<Ident>()
        every {
            personopplysningerServiceMock.hentAktivAktørId(capture(hentAktørIdIdentSlot))
        } answers {
            AktørId(id = "0${hentAktørIdIdentSlot.captured.ident}")
        }

        every {
            personopplysningerServiceMock.hentStatsborgerskap(any())
        } returns listOf(Statsborgerskap(land = "NOR", gyldigFraOgMed = now.minusYears(20), gyldigTilOgMed = null))

        every {
            personopplysningerServiceMock.hentOpphold(any())
        } returns listOf(Opphold(OPPHOLDSTILLATELSE.PERMANENT, null, null))

        every {
            personopplysningerServiceMock.hentBostedsadresseperioder(any())
        } returns listOf()

        every {
            personopplysningerServiceMock.hentDødsfall(any())
        } returns DødsfallData(false, null)

         every {
            personopplysningerServiceMock.hentVergeData(any())
        } returns VergeData(false)

        return personopplysningerServiceMock
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
