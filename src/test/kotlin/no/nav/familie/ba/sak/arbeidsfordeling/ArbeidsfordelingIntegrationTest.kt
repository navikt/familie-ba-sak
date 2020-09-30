package no.nav.familie.ba.sak.arbeidsfordeling

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.behandling.NyBehandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.behandling.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.lagSøknadDTO
import no.nav.familie.ba.sak.e2e.DatabaseCleanupService
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.oppgave.OppgaveService
import no.nav.familie.ba.sak.pdl.PersonInfoQuery
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.pdl.internal.*
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
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
@ActiveProfiles("postgres", "mock-pdl-arbeidsfordeling")
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

        every { integrasjonClient.hentBehandlendeEnhet(ArbeidsfordelingMockConfiguration.søkerFnr) } returns listOf(
                Arbeidsfordelingsenhet(enhetId = IKKE_FORTROLIG_ENHET,
                                       enhetNavn = "vanlig enhet"))

        every { integrasjonClient.hentBehandlendeEnhet(ArbeidsfordelingMockConfiguration.barnMedDiskresjonskode) } returns listOf(
                Arbeidsfordelingsenhet(enhetId = FORTROLIG_ENHET,
                                       enhetNavn = "Diskresjonsenhet"))
    }

    @Test
    fun `Skal fastsette behandlende enhet ved opprettelse av behandling`() {
        fagsakService.hentEllerOpprettFagsak(PersonIdent(ArbeidsfordelingMockConfiguration.søkerFnr))
        val behandling = stegService.håndterNyBehandling(NyBehandling(
                BehandlingKategori.NASJONAL,
                BehandlingUnderkategori.ORDINÆR,
                ArbeidsfordelingMockConfiguration.søkerFnr,
                BehandlingType.FØRSTEGANGSBEHANDLING
        ))

        val arbeidsfordelingPåBehandling = arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandlingId = behandling.id)

        assertEquals(IKKE_FORTROLIG_ENHET, arbeidsfordelingPåBehandling.behandlendeEnhetId)
    }

    @Test
    fun `Skal ikke fastsette ny behandlende enhet ved registrering av søknad`() {
        fagsakService.hentEllerOpprettFagsak(PersonIdent(ArbeidsfordelingMockConfiguration.søkerFnr))
        val behandling = stegService.håndterNyBehandling(NyBehandling(
                BehandlingKategori.NASJONAL,
                BehandlingUnderkategori.ORDINÆR,
                ArbeidsfordelingMockConfiguration.søkerFnr,
                BehandlingType.FØRSTEGANGSBEHANDLING
        ))

        val arbeidsfordelingPåBehandling = arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandlingId = behandling.id)
        assertEquals(IKKE_FORTROLIG_ENHET, arbeidsfordelingPåBehandling.behandlendeEnhetId)

        stegService.håndterSøknad(behandling, RestRegistrerSøknad(
                søknad = lagSøknadDTO(ArbeidsfordelingMockConfiguration.søkerFnr,
                                      listOf(ArbeidsfordelingMockConfiguration.barnUtenDiskresjonskode)),
                bekreftEndringerViaFrontend = false
        ))

        val arbeidsfordelingPåBehandlingEtterSøknadsregistrering =
                arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandlingId = behandling.id)
        assertEquals(IKKE_FORTROLIG_ENHET, arbeidsfordelingPåBehandlingEtterSøknadsregistrering.behandlendeEnhetId)
    }

    @Test
    fun `Skal fastsette ny behandlende enhet ved registrering av søknad`() {
        fagsakService.hentEllerOpprettFagsak(PersonIdent(ArbeidsfordelingMockConfiguration.søkerFnr))
        val behandling = stegService.håndterNyBehandling(NyBehandling(
                BehandlingKategori.NASJONAL,
                BehandlingUnderkategori.ORDINÆR,
                ArbeidsfordelingMockConfiguration.søkerFnr,
                BehandlingType.FØRSTEGANGSBEHANDLING
        ))

        val arbeidsfordelingPåBehandling = arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandlingId = behandling.id)
        assertEquals(IKKE_FORTROLIG_ENHET, arbeidsfordelingPåBehandling.behandlendeEnhetId)

        stegService.håndterSøknad(behandling, RestRegistrerSøknad(
                søknad = lagSøknadDTO(ArbeidsfordelingMockConfiguration.søkerFnr,
                                      listOf(ArbeidsfordelingMockConfiguration.barnMedDiskresjonskode)),
                bekreftEndringerViaFrontend = false
        ))

        val arbeidsfordelingPåBehandlingEtterSøknadsregistrering =
                arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandlingId = behandling.id)
        assertEquals(FORTROLIG_ENHET, arbeidsfordelingPåBehandlingEtterSøknadsregistrering.behandlendeEnhetId)
    }

    @Test
    fun `Skal fastsette ny behandlende enhet når man legger til nytt barn ved endring på søknadsgrunnlag`() {
        fagsakService.hentEllerOpprettFagsak(PersonIdent(ArbeidsfordelingMockConfiguration.søkerFnr))
        val behandling = stegService.håndterNyBehandling(NyBehandling(
                BehandlingKategori.NASJONAL,
                BehandlingUnderkategori.ORDINÆR,
                ArbeidsfordelingMockConfiguration.søkerFnr,
                BehandlingType.FØRSTEGANGSBEHANDLING
        ))

        val arbeidsfordelingPåBehandling = arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandlingId = behandling.id)
        assertEquals(IKKE_FORTROLIG_ENHET, arbeidsfordelingPåBehandling.behandlendeEnhetId)

        stegService.håndterSøknad(behandling, RestRegistrerSøknad(
                søknad = lagSøknadDTO(ArbeidsfordelingMockConfiguration.søkerFnr,
                                      listOf(ArbeidsfordelingMockConfiguration.barnUtenDiskresjonskode)),
                bekreftEndringerViaFrontend = false
        ))

        val arbeidsfordelingPåBehandlingEtterSøknadsregistreringUtenDiskresjonskode =
                arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandlingId = behandling.id)
        assertEquals(IKKE_FORTROLIG_ENHET,
                     arbeidsfordelingPåBehandlingEtterSøknadsregistreringUtenDiskresjonskode.behandlendeEnhetId)

        stegService.håndterSøknad(behandling, RestRegistrerSøknad(
                søknad = lagSøknadDTO(ArbeidsfordelingMockConfiguration.søkerFnr,
                                      listOf(ArbeidsfordelingMockConfiguration.barnUtenDiskresjonskode,
                                             ArbeidsfordelingMockConfiguration.barnMedDiskresjonskode)),
                bekreftEndringerViaFrontend = false
        ))

        val arbeidsfordelingPåBehandlingEtterSøknadsregistreringMedDiskresjonskode =
                arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandlingId = behandling.id)
        assertEquals(FORTROLIG_ENHET, arbeidsfordelingPåBehandlingEtterSøknadsregistreringMedDiskresjonskode.behandlendeEnhetId)
    }

    @Test
    fun `Skal ikke fastsette ny behandlende enhet ved registrering av søknad når enhet er manuelt satt`() {
        fagsakService.hentEllerOpprettFagsak(PersonIdent(ArbeidsfordelingMockConfiguration.søkerFnr))
        val behandling = stegService.håndterNyBehandling(NyBehandling(
                BehandlingKategori.NASJONAL,
                BehandlingUnderkategori.ORDINÆR,
                ArbeidsfordelingMockConfiguration.søkerFnr,
                BehandlingType.FØRSTEGANGSBEHANDLING
        ))

        val arbeidsfordelingPåBehandling = arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandlingId = behandling.id)
        assertEquals(IKKE_FORTROLIG_ENHET, arbeidsfordelingPåBehandling.behandlendeEnhetId)

        arbeidsfordelingService.manueltOppdaterBehandlendeEnhet(behandling,
                                                                Arbeidsfordelingsenhet(enhetId = MANUELT_OVERSTYRT_ENHET,
                                                                                       enhetNavn = "manuelt overstyrt enhet"))

        stegService.håndterSøknad(behandling, RestRegistrerSøknad(
                søknad = lagSøknadDTO(ArbeidsfordelingMockConfiguration.søkerFnr,
                                      listOf(ArbeidsfordelingMockConfiguration.barnUtenDiskresjonskode)),
                bekreftEndringerViaFrontend = false
        ))

        val arbeidsfordelingPåBehandlingEtterSøknadsregistrering =
                arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandlingId = behandling.id)
        assertEquals(MANUELT_OVERSTYRT_ENHET, arbeidsfordelingPåBehandlingEtterSøknadsregistrering.behandlendeEnhetId)
    }

    @Test
    fun `Skal fastsette ny behandlende enhet og oppdatere eksisterende oppgave ved registrering av søknad`() {
        fagsakService.hentEllerOpprettFagsak(PersonIdent(ArbeidsfordelingMockConfiguration.søkerFnr))
        val behandling = stegService.håndterNyBehandling(NyBehandling(
                BehandlingKategori.NASJONAL,
                BehandlingUnderkategori.ORDINÆR,
                ArbeidsfordelingMockConfiguration.søkerFnr,
                BehandlingType.FØRSTEGANGSBEHANDLING
        ))

        val arbeidsfordelingPåBehandling = arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandlingId = behandling.id)
        assertEquals(IKKE_FORTROLIG_ENHET, arbeidsfordelingPåBehandling.behandlendeEnhetId)

        oppgaveService.opprettOppgave(behandling.id, Oppgavetype.BehandleSak, LocalDate.now())

        stegService.håndterSøknad(behandling, RestRegistrerSøknad(
                søknad = lagSøknadDTO(ArbeidsfordelingMockConfiguration.søkerFnr,
                                      listOf(ArbeidsfordelingMockConfiguration.barnMedDiskresjonskode)),
                bekreftEndringerViaFrontend = false
        ))

        verify(exactly = 1) {
            oppgaveService.oppdaterOppgave(any())
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
            personopplysningerServiceMock.hentPersoninfoMedRelasjoner(søkerFnr)
        } returns PersonInfo(
                fødselsdato = now.minusYears(20),
                navn = "Mor Søker",
                kjønn = Kjønn.KVINNE,
                sivilstand = SIVILSTAND.UGIFT,
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresse = søkerBostedsadresse
        )

        every {
            personopplysningerServiceMock.hentPersoninfo(barnUtenDiskresjonskode, PersonInfoQuery.ENKEL)
        } returns PersonInfo(
                fødselsdato = now.førsteDagIInneværendeMåned(),
                navn = "Gutt Barn",
                kjønn = Kjønn.MANN,
                sivilstand = SIVILSTAND.UGIFT,
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresse = søkerBostedsadresse
        )

        every {
            personopplysningerServiceMock.hentPersoninfoMedRelasjoner(barnUtenDiskresjonskode)
        } returns PersonInfo(
                fødselsdato = now.førsteDagIInneværendeMåned(),
                navn = "Gutt Barn",
                kjønn = Kjønn.MANN,
                sivilstand = SIVILSTAND.UGIFT,
                familierelasjoner = setOf(
                        Familierelasjon(
                                personIdent = Personident(id = søkerFnr),
                                relasjonsrolle = FAMILIERELASJONSROLLE.MOR
                        )),
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresse = søkerBostedsadresse
        )

        every {
            personopplysningerServiceMock.hentPersoninfo(barnMedDiskresjonskode, PersonInfoQuery.ENKEL)
        } returns PersonInfo(
                fødselsdato = now.førsteDagIInneværendeMåned(),
                navn = "Gutt Barn fortrolig",
                kjønn = Kjønn.MANN,
                sivilstand = SIVILSTAND.UGIFT,
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresse = søkerBostedsadresse
        )

        every {
            personopplysningerServiceMock.hentPersoninfoMedRelasjoner(barnMedDiskresjonskode)
        } returns PersonInfo(
                fødselsdato = now.førsteDagIInneværendeMåned(),
                navn = "Gutt Barn fortrolig",
                kjønn = Kjønn.MANN,
                sivilstand = SIVILSTAND.UGIFT,
                familierelasjoner = setOf(
                        Familierelasjon(
                                personIdent = Personident(id = søkerFnr),
                                relasjonsrolle = FAMILIERELASJONSROLLE.MOR
                        )),
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG,
                bostedsadresse = søkerBostedsadresse
        )

        val hentAktørIdIdentSlot = slot<Ident>()
        every {
            personopplysningerServiceMock.hentAktivAktørId(capture(hentAktørIdIdentSlot))
        } answers {
            AktørId(id = "0${hentAktørIdIdentSlot.captured.ident}")
        }

        return personopplysningerServiceMock
    }

    companion object {

        const val søkerFnr = "12445678910"
        const val barnUtenDiskresjonskode = "12345678911"
        const val barnMedDiskresjonskode = "12345678912"

        val søkerBostedsadresse = Bostedsadresse(
                vegadresse = Vegadresse(matrikkelId = 1111, husnummer = null, husbokstav = null,
                                        bruksenhetsnummer = null, adressenavn = null, kommunenummer = null,
                                        tilleggsnavn = null, postnummer = "2222")
        )

    }

}