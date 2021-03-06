package no.nav.familie.ba.sak.kjerne.arbeidsfordeling

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.GrBostedsadresseperiode
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.lagSøknadDTO
import no.nav.familie.ba.sak.config.e2e.DatabaseCleanupService
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.*
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import no.nav.familie.kontrakter.felles.personopplysning.OPPHOLDSTILLATELSE
import no.nav.familie.kontrakter.felles.personopplysning.Opphold
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import no.nav.familie.kontrakter.felles.personopplysning.Sivilstand
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import org.junit.jupiter.api.Assertions.assertEquals
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
@ActiveProfiles("postgres", "mock-pdl-arbeidsfordeling", "mock-infotrygd-barnetrygd")
@Tag("integration")
class ArbeidsfordelingIntegrationTest(
        @Autowired
        private val fagsakService: FagsakService,

        @Autowired
        private val stegService: StegService,

        @Autowired
        private val arbeidsfordelingService: ArbeidsfordelingService,

        @Autowired
        private val integrasjonClient: IntegrasjonClient,

        @Autowired
        private val oppgaveService: OppgaveService,

        @Autowired
        private val databaseCleanupService: DatabaseCleanupService
) {

    @BeforeEach
    fun init() {
        databaseCleanupService.truncate()

        every { integrasjonClient.hentBehandlendeEnhet(ArbeidsfordelingMockConfiguration.SØKER_FNR) } returns listOf(
                Arbeidsfordelingsenhet(enhetId = IKKE_FORTROLIG_ENHET,
                                       enhetNavn = "vanlig enhet"))

        every { integrasjonClient.hentBehandlendeEnhet(ArbeidsfordelingMockConfiguration.BARN_MED_DISKRESJONSKODE) } returns listOf(
                Arbeidsfordelingsenhet(enhetId = FORTROLIG_ENHET,
                                       enhetNavn = "Diskresjonsenhet"))
    }

    @Test
    fun `Skal fastsette behandlende enhet ved opprettelse av behandling`() {
        fagsakService.hentEllerOpprettFagsak(PersonIdent(ArbeidsfordelingMockConfiguration.SØKER_FNR))
        val behandling = stegService.håndterNyBehandling(NyBehandling(
                BehandlingKategori.NASJONAL,
                BehandlingUnderkategori.ORDINÆR,
                ArbeidsfordelingMockConfiguration.SØKER_FNR,
                BehandlingType.FØRSTEGANGSBEHANDLING
        ))

        val arbeidsfordelingPåBehandling = arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandlingId = behandling.id)

        assertEquals(IKKE_FORTROLIG_ENHET, arbeidsfordelingPåBehandling.behandlendeEnhetId)
    }

    @Test
    fun `Skal ikke fastsette ny behandlende enhet ved registrering av søknad`() {
        fagsakService.hentEllerOpprettFagsak(PersonIdent(ArbeidsfordelingMockConfiguration.SØKER_FNR))
        val behandling = stegService.håndterNyBehandling(NyBehandling(
                BehandlingKategori.NASJONAL,
                BehandlingUnderkategori.ORDINÆR,
                ArbeidsfordelingMockConfiguration.SØKER_FNR,
                BehandlingType.FØRSTEGANGSBEHANDLING
        ))

        val arbeidsfordelingPåBehandling = arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandlingId = behandling.id)
        assertEquals(IKKE_FORTROLIG_ENHET, arbeidsfordelingPåBehandling.behandlendeEnhetId)

        stegService.håndterSøknad(behandling, RestRegistrerSøknad(
                søknad = lagSøknadDTO(ArbeidsfordelingMockConfiguration.SØKER_FNR,
                                      listOf(ArbeidsfordelingMockConfiguration.BARN_UTEN_DISKRESJONSKODE)),
                bekreftEndringerViaFrontend = false
        ))

        val arbeidsfordelingPåBehandlingEtterSøknadsregistrering =
                arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandlingId = behandling.id)
        assertEquals(IKKE_FORTROLIG_ENHET, arbeidsfordelingPåBehandlingEtterSøknadsregistrering.behandlendeEnhetId)
    }

    @Test
    fun `Skal fastsette ny behandlende enhet ved registrering av søknad`() {
        fagsakService.hentEllerOpprettFagsak(PersonIdent(ArbeidsfordelingMockConfiguration.SØKER_FNR))
        val behandling = stegService.håndterNyBehandling(NyBehandling(
                BehandlingKategori.NASJONAL,
                BehandlingUnderkategori.ORDINÆR,
                ArbeidsfordelingMockConfiguration.SØKER_FNR,
                BehandlingType.FØRSTEGANGSBEHANDLING
        ))

        val arbeidsfordelingPåBehandling = arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandlingId = behandling.id)
        assertEquals(IKKE_FORTROLIG_ENHET, arbeidsfordelingPåBehandling.behandlendeEnhetId)

        stegService.håndterSøknad(behandling, RestRegistrerSøknad(
                søknad = lagSøknadDTO(ArbeidsfordelingMockConfiguration.SØKER_FNR,
                                      listOf(ArbeidsfordelingMockConfiguration.BARN_MED_DISKRESJONSKODE)),
                bekreftEndringerViaFrontend = false
        ))

        val arbeidsfordelingPåBehandlingEtterSøknadsregistrering =
                arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandlingId = behandling.id)
        assertEquals(FORTROLIG_ENHET, arbeidsfordelingPåBehandlingEtterSøknadsregistrering.behandlendeEnhetId)
    }

    @Test
    fun `Skal fastsette ny behandlende enhet når man legger til nytt barn ved endring på søknadsgrunnlag`() {
        fagsakService.hentEllerOpprettFagsak(PersonIdent(ArbeidsfordelingMockConfiguration.SØKER_FNR))
        val behandling = stegService.håndterNyBehandling(NyBehandling(
                BehandlingKategori.NASJONAL,
                BehandlingUnderkategori.ORDINÆR,
                ArbeidsfordelingMockConfiguration.SØKER_FNR,
                BehandlingType.FØRSTEGANGSBEHANDLING
        ))

        val arbeidsfordelingPåBehandling = arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandlingId = behandling.id)
        assertEquals(IKKE_FORTROLIG_ENHET, arbeidsfordelingPåBehandling.behandlendeEnhetId)

        stegService.håndterSøknad(behandling, RestRegistrerSøknad(
                søknad = lagSøknadDTO(ArbeidsfordelingMockConfiguration.SØKER_FNR,
                                      listOf(ArbeidsfordelingMockConfiguration.BARN_UTEN_DISKRESJONSKODE)),
                bekreftEndringerViaFrontend = false
        ))

        val arbeidsfordelingPåBehandlingEtterSøknadsregistreringUtenDiskresjonskode =
                arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandlingId = behandling.id)
        assertEquals(IKKE_FORTROLIG_ENHET,
                     arbeidsfordelingPåBehandlingEtterSøknadsregistreringUtenDiskresjonskode.behandlendeEnhetId)

        stegService.håndterSøknad(behandling, RestRegistrerSøknad(
                søknad = lagSøknadDTO(ArbeidsfordelingMockConfiguration.SØKER_FNR,
                                      listOf(ArbeidsfordelingMockConfiguration.BARN_UTEN_DISKRESJONSKODE,
                                             ArbeidsfordelingMockConfiguration.BARN_MED_DISKRESJONSKODE)),
                bekreftEndringerViaFrontend = false
        ))

        val arbeidsfordelingPåBehandlingEtterSøknadsregistreringMedDiskresjonskode =
                arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandlingId = behandling.id)
        assertEquals(FORTROLIG_ENHET, arbeidsfordelingPåBehandlingEtterSøknadsregistreringMedDiskresjonskode.behandlendeEnhetId)
    }

    @Test
    fun `Skal ikke fastsette ny behandlende enhet ved registrering av søknad når enhet er manuelt satt`() {
        fagsakService.hentEllerOpprettFagsak(PersonIdent(ArbeidsfordelingMockConfiguration.SØKER_FNR))
        val behandling = stegService.håndterNyBehandling(NyBehandling(
                BehandlingKategori.NASJONAL,
                BehandlingUnderkategori.ORDINÆR,
                ArbeidsfordelingMockConfiguration.SØKER_FNR,
                BehandlingType.FØRSTEGANGSBEHANDLING
        ))

        val arbeidsfordelingPåBehandling = arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandlingId = behandling.id)
        assertEquals(IKKE_FORTROLIG_ENHET, arbeidsfordelingPåBehandling.behandlendeEnhetId)

        arbeidsfordelingService.manueltOppdaterBehandlendeEnhet(behandling,
                                                                RestEndreBehandlendeEnhet(
                                                                        enhetId = MANUELT_OVERSTYRT_ENHET,
                                                                        begrunnelse = ""))

        stegService.håndterSøknad(behandling,
                                  RestRegistrerSøknad(
                                          søknad = lagSøknadDTO(ArbeidsfordelingMockConfiguration.SØKER_FNR,
                                                                listOf(ArbeidsfordelingMockConfiguration.BARN_UTEN_DISKRESJONSKODE)),
                                          bekreftEndringerViaFrontend = false
                                  ))

        val arbeidsfordelingPåBehandlingEtterSøknadsregistrering =
                arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandlingId = behandling.id)
        assertEquals(MANUELT_OVERSTYRT_ENHET, arbeidsfordelingPåBehandlingEtterSøknadsregistrering.behandlendeEnhetId)
    }

    @Test
    fun `Skal fastsette ny behandlende enhet og oppdatere eksisterende oppgave ved registrering av søknad`() {
        fagsakService.hentEllerOpprettFagsak(PersonIdent(ArbeidsfordelingMockConfiguration.SØKER_FNR))
        val behandling = stegService.håndterNyBehandling(NyBehandling(
                BehandlingKategori.NASJONAL,
                BehandlingUnderkategori.ORDINÆR,
                ArbeidsfordelingMockConfiguration.SØKER_FNR,
                BehandlingType.FØRSTEGANGSBEHANDLING
        ))

        val arbeidsfordelingPåBehandling = arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandlingId = behandling.id)
        assertEquals(IKKE_FORTROLIG_ENHET, arbeidsfordelingPåBehandling.behandlendeEnhetId)

        oppgaveService.opprettOppgave(behandling.id, Oppgavetype.BehandleSak, LocalDate.now())

        stegService.håndterSøknad(behandling, RestRegistrerSøknad(
                søknad = lagSøknadDTO(ArbeidsfordelingMockConfiguration.SØKER_FNR,
                                      listOf(ArbeidsfordelingMockConfiguration.BARN_MED_DISKRESJONSKODE)),
                bekreftEndringerViaFrontend = false
        ))

        verify(exactly = 1) {
            oppgaveService.patchOppgave(any())
        }

        val arbeidsfordelingPåBehandlingEtterSøknadsregistreringUtenDiskresjonskode =
                arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandlingId = behandling.id)
        assertEquals(FORTROLIG_ENHET,
                     arbeidsfordelingPåBehandlingEtterSøknadsregistreringUtenDiskresjonskode.behandlendeEnhetId)
    }

    companion object {

        const val MANUELT_OVERSTYRT_ENHET = "1234"
        const val IKKE_FORTROLIG_ENHET = "4820"
        const val FORTROLIG_ENHET = "1122"
    }
}

@Configuration
class ArbeidsfordelingMockConfiguration {

    val now: LocalDate = LocalDate.now()

    @Bean
    @Profile("mock-pdl-arbeidsfordeling")
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
            personopplysningerServiceMock.hentPersoninfoMedRelasjoner(SØKER_FNR)
        } returns PersonInfo(
                fødselsdato = now.minusYears(20),
                navn = "Mor Søker",
                kjønn = Kjønn.KVINNE,
                sivilstander = listOf(Sivilstand(type = SIVILSTAND.UGIFT)),
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresser = mutableListOf(søkerBostedsadresse)
        )

        every {
            personopplysningerServiceMock.hentPersoninfo(BARN_UTEN_DISKRESJONSKODE)
        } returns PersonInfo(
                fødselsdato = now.førsteDagIInneværendeMåned(),
                navn = "Gutt Barn",
                kjønn = Kjønn.MANN,
                sivilstander = listOf(Sivilstand(type = SIVILSTAND.UGIFT)),
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresser = mutableListOf(søkerBostedsadresse)
        )

        every {
            personopplysningerServiceMock.hentPersoninfoMedRelasjoner(BARN_UTEN_DISKRESJONSKODE)
        } returns PersonInfo(
                fødselsdato = now.førsteDagIInneværendeMåned(),
                navn = "Gutt Barn",
                kjønn = Kjønn.MANN,
                sivilstander = listOf(Sivilstand(type = SIVILSTAND.UGIFT)),
                forelderBarnRelasjon = setOf(
                        ForelderBarnRelasjon(
                                personIdent = Personident(id = SØKER_FNR),
                                relasjonsrolle = FORELDERBARNRELASJONROLLE.MOR
                        )),
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresser = mutableListOf(søkerBostedsadresse)
        )

        every {
            personopplysningerServiceMock.hentPersoninfo(BARN_MED_DISKRESJONSKODE)
        } returns PersonInfo(
                fødselsdato = now.førsteDagIInneværendeMåned(),
                navn = "Gutt Barn fortrolig",
                kjønn = Kjønn.MANN,
                sivilstander = listOf(Sivilstand(type = SIVILSTAND.UGIFT)),
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresser = mutableListOf(søkerBostedsadresse)
        )

        every {
            personopplysningerServiceMock.hentPersoninfoMedRelasjoner(BARN_MED_DISKRESJONSKODE)
        } returns PersonInfo(
                fødselsdato = now.førsteDagIInneværendeMåned(),
                navn = "Gutt Barn fortrolig",
                kjønn = Kjønn.MANN,
                sivilstander = listOf(Sivilstand(type = SIVILSTAND.UGIFT)),
                forelderBarnRelasjon = setOf(
                        ForelderBarnRelasjon(
                                personIdent = Personident(id = SØKER_FNR),
                                relasjonsrolle = FORELDERBARNRELASJONROLLE.MOR
                        )),
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG,
                bostedsadresser = mutableListOf(søkerBostedsadresse)
        )

        every {
            personopplysningerServiceMock.hentHistoriskPersoninfoManuell(any())
        } returns PersonInfo(fødselsdato = now, navn = "")

        val hentAktørIdIdentSlot = slot<Ident>()
        every {
            personopplysningerServiceMock.hentAktivAktørId(capture(hentAktørIdIdentSlot))
        } answers {
            AktørId(id = "0${hentAktørIdIdentSlot.captured.ident}")
        }

        every { personopplysningerServiceMock.hentStatsborgerskap(any()) } returns listOf(Statsborgerskap(land = "NOR",
                                                                                                          LocalDate.now(),
                                                                                                          LocalDate.now()))

        every { personopplysningerServiceMock.hentBostedsadresseperioder(any()) } returns listOf(GrBostedsadresseperiode(0,
                                                                                                                         DatoIntervallEntitet(
                                                                                                                                 LocalDate.now(),
                                                                                                                                 LocalDate.now())))

        every { personopplysningerServiceMock.hentOpphold(any()) } returns listOf(Opphold(OPPHOLDSTILLATELSE.PERMANENT,
                                                                                          LocalDate.now(),
                                                                                          LocalDate.now()))

        return personopplysningerServiceMock
    }

    companion object {

        const val SØKER_FNR = "12445678910"
        const val BARN_UTEN_DISKRESJONSKODE = "12345678911"
        const val BARN_MED_DISKRESJONSKODE = "12345678912"

        val søkerBostedsadresse = Bostedsadresse(
                vegadresse = Vegadresse(matrikkelId = 1111, husnummer = null, husbokstav = null,
                                        bruksenhetsnummer = null, adressenavn = null, kommunenummer = null,
                                        tilleggsnavn = null, postnummer = "2222")
        )

    }

}