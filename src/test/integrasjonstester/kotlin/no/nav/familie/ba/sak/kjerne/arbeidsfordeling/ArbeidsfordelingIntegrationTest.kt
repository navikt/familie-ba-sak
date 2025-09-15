package no.nav.familie.ba.sak.kjerne.arbeidsfordeling

import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.config.MockPersonopplysningerService.Companion.leggTilPersonInfo
import no.nav.familie.ba.sak.config.MockPersonopplysningerService.Companion.leggTilRelasjonIPersonInfo
import no.nav.familie.ba.sak.datagenerator.lagBostedsadresse
import no.nav.familie.ba.sak.datagenerator.lagSøknadDTO
import no.nav.familie.ba.sak.datagenerator.randomBarnFnr
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.kontrakter.felles.navkontor.NavKontorEnhet
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE.MOR
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTANDTYPE
import no.nav.familie.kontrakter.felles.personopplysning.Sivilstand
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate.now

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
    private val personidentService: PersonidentService,
    @Autowired
    private val databaseCleanupService: DatabaseCleanupService,
) : AbstractSpringIntegrationTest() {
    @BeforeEach
    fun init() {
        databaseCleanupService.truncate()

        val now = now()

        leggTilPersonInfo(
            SØKER_FNR,
            PersonInfo(
                fødselsdato = now.minusYears(20),
                navn = "Mor Søker",
                kjønn = Kjønn.KVINNE,
                sivilstander = listOf(Sivilstand(type = SIVILSTANDTYPE.UGIFT)),
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresser = søkerBostedsadresse,
            ),
        )

        leggTilPersonInfo(
            BARN_UTEN_DISKRESJONSKODE,
            PersonInfo(
                fødselsdato = now.førsteDagIInneværendeMåned(),
                navn = "Gutt Barn",
                kjønn = Kjønn.MANN,
                sivilstander = listOf(Sivilstand(type = SIVILSTANDTYPE.UGIFT)),
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresser = søkerBostedsadresse,
            ),
        )
        leggTilRelasjonIPersonInfo(BARN_UTEN_DISKRESJONSKODE, SØKER_FNR, MOR)

        leggTilPersonInfo(
            BARN_MED_DISKRESJONSKODE,
            PersonInfo(
                fødselsdato = now.førsteDagIInneværendeMåned(),
                navn = "Gutt Barn fortrolig",
                kjønn = Kjønn.MANN,
                sivilstander = listOf(Sivilstand(type = SIVILSTANDTYPE.UGIFT)),
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG,
                bostedsadresser = søkerBostedsadresse,
            ),
        )
        leggTilRelasjonIPersonInfo(BARN_MED_DISKRESJONSKODE, SØKER_FNR, MOR)

        every { integrasjonClient.hentBehandlendeEnhet(eq(SØKER_FNR)) } returns
            listOf(
                Arbeidsfordelingsenhet(
                    enhetId = IKKE_FORTROLIG_ENHET.enhetsnummer,
                    enhetNavn = "vanlig enhet",
                ),
            )

        every { integrasjonClient.hentBehandlendeEnhet(eq(BARN_MED_DISKRESJONSKODE)) } returns
            listOf(
                Arbeidsfordelingsenhet(
                    enhetId = FORTROLIG_ENHET.enhetsnummer,
                    enhetNavn = "Diskresjonsenhet",
                ),
            )
        val hentEnhetSlot = slot<String>()
        every { integrasjonClient.hentEnhet(capture(hentEnhetSlot)) } answers {
            NavKontorEnhet(
                enhetId = hentEnhetSlot.captured.toInt(),
                navn = "${hentEnhetSlot.captured}, NAV Familie- og pensjonsytelser Oslo 1",
                enhetNr = "4848",
                status = "aktiv",
            )
        }
    }

    @Test
    fun `Skal fastsette behandlende enhet ved opprettelse av behandling`() {
        val søkerAktør = personidentService.hentAktør(SØKER_FNR)
        val fagsak =
            fagsakService.hentEllerOpprettFagsak(
                søkerAktør.aktivFødselsnummer(),
            )
        val behandling =
            stegService.håndterNyBehandling(
                lagNyBehandling(fagsak.id),
            )

        val arbeidsfordelingPåBehandling =
            arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id)

        assertEquals(IKKE_FORTROLIG_ENHET.enhetsnummer, arbeidsfordelingPåBehandling.behandlendeEnhetId)
    }

    @Test
    fun `Skal ikke fastsette ny behandlende enhet ved registrering av søknad`() {
        val søkerAktør = personidentService.hentAktør(SØKER_FNR)
        val fagsak =
            fagsakService.hentEllerOpprettFagsak(
                søkerAktør.aktivFødselsnummer(),
            )
        val behandling =
            stegService.håndterNyBehandling(
                lagNyBehandling(fagsak.id),
            )

        val arbeidsfordelingPåBehandling =
            arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id)
        assertEquals(IKKE_FORTROLIG_ENHET.enhetsnummer, arbeidsfordelingPåBehandling.behandlendeEnhetId)

        stegService.håndterSøknad(
            behandling,
            RestRegistrerSøknad(
                søknad =
                    lagSøknadDTO(
                        SØKER_FNR,
                        listOf(BARN_UTEN_DISKRESJONSKODE),
                    ),
                bekreftEndringerViaFrontend = false,
            ),
        )

        val arbeidsfordelingPåBehandlingEtterSøknadsregistrering =
            arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id)
        assertEquals(IKKE_FORTROLIG_ENHET, arbeidsfordelingPåBehandlingEtterSøknadsregistrering.behandlendeEnhetId)
    }

    @Test
    fun `Skal fastsette ny behandlende enhet ved registrering av søknad`() {
        val søkerAktør = personidentService.hentAktør(SØKER_FNR)
        val fagsak =
            fagsakService.hentEllerOpprettFagsak(
                søkerAktør.aktivFødselsnummer(),
            )
        val behandling =
            stegService.håndterNyBehandling(
                lagNyBehandling(fagsak.id),
            )

        val arbeidsfordelingPåBehandling =
            arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id)
        assertEquals(IKKE_FORTROLIG_ENHET.enhetsnummer, arbeidsfordelingPåBehandling.behandlendeEnhetId)

        stegService.håndterSøknad(
            behandling,
            RestRegistrerSøknad(
                søknad =
                    lagSøknadDTO(
                        SØKER_FNR,
                        listOf(BARN_MED_DISKRESJONSKODE),
                    ),
                bekreftEndringerViaFrontend = false,
            ),
        )

        val arbeidsfordelingPåBehandlingEtterSøknadsregistrering =
            arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id)
        assertEquals(FORTROLIG_ENHET, arbeidsfordelingPåBehandlingEtterSøknadsregistrering.behandlendeEnhetId)
    }

    @Test
    fun `Skal fastsette ny behandlende enhet når man legger til nytt barn ved endring på søknadsgrunnlag`() {
        val søkerAktør = personidentService.hentAktør(SØKER_FNR)
        val fagsak =
            fagsakService.hentEllerOpprettFagsak(
                søkerAktør.aktivFødselsnummer(),
            )
        val behandling =
            stegService.håndterNyBehandling(
                lagNyBehandling(fagsak.id),
            )

        val arbeidsfordelingPåBehandling =
            arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id)
        assertEquals(IKKE_FORTROLIG_ENHET.enhetsnummer, arbeidsfordelingPåBehandling.behandlendeEnhetId)

        stegService.håndterSøknad(
            behandling,
            RestRegistrerSøknad(
                søknad =
                    lagSøknadDTO(
                        SØKER_FNR,
                        listOf(BARN_UTEN_DISKRESJONSKODE),
                    ),
                bekreftEndringerViaFrontend = false,
            ),
        )

        val arbeidsfordelingPåBehandlingEtterSøknadsregistreringUtenDiskresjonskode =
            arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id)
        assertEquals(
            IKKE_FORTROLIG_ENHET,
            arbeidsfordelingPåBehandlingEtterSøknadsregistreringUtenDiskresjonskode.behandlendeEnhetId,
        )

        stegService.håndterSøknad(
            behandling,
            RestRegistrerSøknad(
                søknad =
                    lagSøknadDTO(
                        SØKER_FNR,
                        listOf(
                            BARN_UTEN_DISKRESJONSKODE,
                            BARN_MED_DISKRESJONSKODE,
                        ),
                    ),
                bekreftEndringerViaFrontend = false,
            ),
        )

        val arbeidsfordelingPåBehandlingEtterSøknadsregistreringMedDiskresjonskode =
            arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id)
        assertEquals(
            FORTROLIG_ENHET,
            arbeidsfordelingPåBehandlingEtterSøknadsregistreringMedDiskresjonskode.behandlendeEnhetId,
        )
    }

    @Test
    fun `Skal ikke fastsette ny behandlende enhet ved registrering av søknad når enhet er manuelt satt`() {
        val søkerAktør = personidentService.hentAktør(SØKER_FNR)
        val fagsak =
            fagsakService.hentEllerOpprettFagsak(
                søkerAktør.aktivFødselsnummer(),
            )
        val behandling =
            stegService.håndterNyBehandling(
                lagNyBehandling(fagsak.id),
            )

        val arbeidsfordelingPåBehandling =
            arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id)
        assertEquals(IKKE_FORTROLIG_ENHET.enhetsnummer, arbeidsfordelingPåBehandling.behandlendeEnhetId)

        arbeidsfordelingService.manueltOppdaterBehandlendeEnhet(
            behandling,
            RestEndreBehandlendeEnhet(
                enhetId = MANUELT_OVERSTYRT_ENHET.enhetsnummer,
                begrunnelse = "",
            ),
        )

        stegService.håndterSøknad(
            behandling,
            RestRegistrerSøknad(
                søknad =
                    lagSøknadDTO(
                        SØKER_FNR,
                        listOf(BARN_UTEN_DISKRESJONSKODE),
                    ),
                bekreftEndringerViaFrontend = false,
            ),
        )

        val arbeidsfordelingPåBehandlingEtterSøknadsregistrering =
            arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id)
        assertEquals(MANUELT_OVERSTYRT_ENHET, arbeidsfordelingPåBehandlingEtterSøknadsregistrering.behandlendeEnhetId)
    }

    @Test
    fun `Skal fastsette ny behandlende enhet og oppdatere eksisterende oppgave ved registrering av søknad`() {
        val søkerAktør = personidentService.hentAktør(SØKER_FNR)
        val fagsak =
            fagsakService.hentEllerOpprettFagsak(
                søkerAktør.aktivFødselsnummer(),
            )
        val behandling =
            stegService.håndterNyBehandling(
                lagNyBehandling(fagsak.id),
            )

        val arbeidsfordelingPåBehandling =
            arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id)
        assertEquals(IKKE_FORTROLIG_ENHET.enhetsnummer, arbeidsfordelingPåBehandling.behandlendeEnhetId)

        val oppgaveId = oppgaveService.opprettOppgave(behandling.id, Oppgavetype.BehandleSak, now())

        stegService.håndterSøknad(
            behandling,
            RestRegistrerSøknad(
                søknad =
                    lagSøknadDTO(
                        SØKER_FNR,
                        listOf(BARN_MED_DISKRESJONSKODE),
                    ),
                bekreftEndringerViaFrontend = false,
            ),
        )

        verify(exactly = 1) {
            integrasjonClient.tilordneEnhetOgRessursForOppgave(any(), any())
        }

        val arbeidsfordelingPåBehandlingEtterSøknadsregistreringUtenDiskresjonskode =
            arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id)
        assertEquals(
            FORTROLIG_ENHET,
            arbeidsfordelingPåBehandlingEtterSøknadsregistreringUtenDiskresjonskode.behandlendeEnhetId,
        )
    }

    private fun lagNyBehandling(fagsakId: Long) =
        NyBehandling(
            kategori = BehandlingKategori.NASJONAL,
            underkategori = BehandlingUnderkategori.ORDINÆR,
            søkersIdent = SØKER_FNR,
            behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
            søknadMottattDato = now(),
            fagsakId = fagsakId,
        )

    private fun mockBarnMedRelasjonOgGradering(
        relasjonFnr: String,
        adressebeskyttelseGradering: ADRESSEBESKYTTELSEGRADERING,
    ): String {
        val barnFnr =
            leggTilPersonInfo(
                randomBarnFnr(),
                PersonInfo(
                    fødselsdato = now().førsteDagIInneværendeMåned(),
                    navn = "Barn Mockesen",
                    kjønn = Kjønn.MANN,
                    sivilstander = listOf(Sivilstand(type = SIVILSTANDTYPE.UGIFT)),
                    adressebeskyttelseGradering = adressebeskyttelseGradering,
                    bostedsadresser = søkerBostedsadresse,
                ),
            )
        leggTilRelasjonIPersonInfo(barnFnr, relasjonFnr, FORELDERBARNRELASJONROLLE.MOR)
        return barnFnr
    }

    private fun mockBehandlendeEnhetForPerson(
        fnr: String,
        enhet: BarnetrygdEnhet,
    ) {
        every { integrasjonClient.hentBehandlendeEnhet(eq(fnr)) } returns listOf(Arbeidsfordelingsenhet.opprettFra(enhet))
    }

    companion object {
        val MANUELT_OVERSTYRT_ENHET = BarnetrygdEnhet.OSLO
        val IKKE_FORTROLIG_ENHET = BarnetrygdEnhet.DRAMMEN
        val FORTROLIG_ENHET = BarnetrygdEnhet.VIKAFOSSEN
        val søkerBostedsadresse = listOf(lagBostedsadresse())
        val SØKER_FNR = randomFnr()
        val BARN_UTEN_DISKRESJONSKODE = randomFnr()
        val BARN_MED_DISKRESJONSKODE = randomFnr()
    }
}
