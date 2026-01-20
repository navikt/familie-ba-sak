package no.nav.familie.ba.sak.kjerne.arbeidsfordeling

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.lagBostedsadresse
import no.nav.familie.ba.sak.datagenerator.lagSøknadDTO
import no.nav.familie.ba.sak.datagenerator.randomBarnFødselsdato
import no.nav.familie.ba.sak.datagenerator.randomSøkerFødselsdato
import no.nav.familie.ba.sak.ekstern.restDomene.RegistrerSøknadDto
import no.nav.familie.ba.sak.fake.FakeIntegrasjonKlient
import no.nav.familie.ba.sak.fake.FakePersonopplysningerService.Companion.leggTilPersonInfo
import no.nav.familie.ba.sak.fake.FakePersonopplysningerService.Companion.leggTilRelasjonIPersonInfo
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTANDTYPE
import no.nav.familie.kontrakter.felles.personopplysning.Sivilstand
import org.junit.jupiter.api.Assertions.assertEquals
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
    private val fakeIntegrasjonKlient: FakeIntegrasjonKlient,
    @Autowired
    private val oppgaveService: OppgaveService,
) : AbstractSpringIntegrationTest() {
    @Test
    fun `Skal fastsette behandlende enhet ved opprettelse av behandling`() {
        // Arrange
        val søkerFnr = mockSøker()
        fakeIntegrasjonKlient.leggTilBehandlendeEnhet(
            søkerFnr,
            listOf(IKKE_FORTROLIG_ENHET),
        )

        val fagsak = fagsakService.hentEllerOpprettFagsak(søkerFnr)

        // Act
        val behandling =
            stegService.håndterNyBehandling(
                lagNyBehandling(fagsak.id, søkerFnr),
            )

        // Assert
        val arbeidsfordelingPåBehandling =
            arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id)

        assertEquals(IKKE_FORTROLIG_ENHET.enhetsnummer, arbeidsfordelingPåBehandling.behandlendeEnhetId)
    }

    @Test
    fun `Skal ikke fastsette ny behandlende enhet ved registrering av søknad`() {
        // Arrange
        val søkerFnr = mockSøker()
        val ikkeFortreligBarnFnr = mockBarnMedRelasjonOgGradering(søkerFnr, ADRESSEBESKYTTELSEGRADERING.UGRADERT)
        fakeIntegrasjonKlient.leggTilBehandlendeEnhet(
            søkerFnr,
            listOf(IKKE_FORTROLIG_ENHET),
        )
        fakeIntegrasjonKlient.leggTilBehandlendeEnhet(
            ikkeFortreligBarnFnr,
            listOf(IKKE_FORTROLIG_ENHET),
        )

        val fagsak =
            fagsakService.hentEllerOpprettFagsak(
                søkerFnr,
            )
        val behandling =
            stegService.håndterNyBehandling(
                lagNyBehandling(fagsak.id, søkerFnr),
            )

        val arbeidsfordelingPåBehandling =
            arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id)
        assertEquals(IKKE_FORTROLIG_ENHET.enhetsnummer, arbeidsfordelingPåBehandling.behandlendeEnhetId)

        // Act
        stegService.håndterSøknad(
            behandling,
            RegistrerSøknadDto(
                søknad =
                    lagSøknadDTO(
                        søkerFnr,
                        listOf(ikkeFortreligBarnFnr),
                    ),
                bekreftEndringerViaFrontend = false,
            ),
        )

        // Assert
        val arbeidsfordelingPåBehandlingEtterSøknadsregistrering =
            arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id)
        assertEquals(IKKE_FORTROLIG_ENHET.enhetsnummer, arbeidsfordelingPåBehandlingEtterSøknadsregistrering.behandlendeEnhetId)
    }

    @Test
    fun `Skal fastsette ny behandlende enhet ved registrering av søknad`() {
        // Arrange
        val søkerFnr = mockSøker()
        val fortroligBarnFnr = mockBarnMedRelasjonOgGradering(søkerFnr, ADRESSEBESKYTTELSEGRADERING.FORTROLIG)
        fakeIntegrasjonKlient.leggTilBehandlendeEnhet(
            søkerFnr,
            listOf(IKKE_FORTROLIG_ENHET),
        )
        fakeIntegrasjonKlient.leggTilBehandlendeEnhet(
            fortroligBarnFnr,
            listOf(FORTROLIG_ENHET),
        )

        val fagsak = fagsakService.hentEllerOpprettFagsak(søkerFnr)
        val behandling =
            stegService.håndterNyBehandling(
                lagNyBehandling(fagsak.id, søkerFnr),
            )

        val arbeidsfordelingPåBehandling =
            arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id)
        assertEquals(IKKE_FORTROLIG_ENHET.enhetsnummer, arbeidsfordelingPåBehandling.behandlendeEnhetId)

        // Act
        stegService.håndterSøknad(
            behandling,
            RegistrerSøknadDto(
                søknad =
                    lagSøknadDTO(
                        søkerFnr,
                        listOf(fortroligBarnFnr),
                    ),
                bekreftEndringerViaFrontend = false,
            ),
        )

        // Assert
        val arbeidsfordelingPåBehandlingEtterSøknadsregistrering =
            arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id)
        assertEquals(FORTROLIG_ENHET.enhetsnummer, arbeidsfordelingPåBehandlingEtterSøknadsregistrering.behandlendeEnhetId)
    }

    @Test
    fun `Skal fastsette ny behandlende enhet når man legger til nytt barn ved endring på søknadsgrunnlag`() {
        // Assert
        val søkerFnr = mockSøker()
        val ugradertBarnFnr = mockBarnMedRelasjonOgGradering(søkerFnr, ADRESSEBESKYTTELSEGRADERING.UGRADERT)
        val fortroligBarnFnr = mockBarnMedRelasjonOgGradering(søkerFnr, ADRESSEBESKYTTELSEGRADERING.FORTROLIG)
        fakeIntegrasjonKlient.leggTilBehandlendeEnhet(
            søkerFnr,
            listOf(IKKE_FORTROLIG_ENHET),
        )
        fakeIntegrasjonKlient.leggTilBehandlendeEnhet(
            ugradertBarnFnr,
            listOf(IKKE_FORTROLIG_ENHET),
        )
        fakeIntegrasjonKlient.leggTilBehandlendeEnhet(
            fortroligBarnFnr,
            listOf(FORTROLIG_ENHET),
        )

        val fagsak = fagsakService.hentEllerOpprettFagsak(søkerFnr)
        val behandling =
            stegService.håndterNyBehandling(
                lagNyBehandling(fagsak.id, søkerFnr),
            )

        val arbeidsfordelingPåBehandling =
            arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id)
        assertEquals(IKKE_FORTROLIG_ENHET.enhetsnummer, arbeidsfordelingPåBehandling.behandlendeEnhetId)

        stegService.håndterSøknad(
            behandling,
            RegistrerSøknadDto(
                søknad =
                    lagSøknadDTO(
                        søkerFnr,
                        listOf(ugradertBarnFnr),
                    ),
                bekreftEndringerViaFrontend = false,
            ),
        )

        val arbeidsfordelingPåBehandlingEtterSøknadsregistreringUtenDiskresjonskode =
            arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id)
        assertEquals(
            IKKE_FORTROLIG_ENHET.enhetsnummer,
            arbeidsfordelingPåBehandlingEtterSøknadsregistreringUtenDiskresjonskode.behandlendeEnhetId,
        )

        // Act
        stegService.håndterSøknad(
            behandling,
            RegistrerSøknadDto(
                søknad =
                    lagSøknadDTO(
                        søkerFnr,
                        listOf(
                            ugradertBarnFnr,
                            fortroligBarnFnr,
                        ),
                    ),
                bekreftEndringerViaFrontend = false,
            ),
        )

        // Assert
        val arbeidsfordelingPåBehandlingEtterSøknadsregistreringMedDiskresjonskode =
            arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id)
        assertEquals(
            FORTROLIG_ENHET.enhetsnummer,
            arbeidsfordelingPåBehandlingEtterSøknadsregistreringMedDiskresjonskode.behandlendeEnhetId,
        )
    }

    @Test
    fun `Skal ikke fastsette ny behandlende enhet ved registrering av søknad når enhet er manuelt satt`() {
        // Arrange
        val søkerFnr = mockSøker()
        val ugradertBarnFnr = mockBarnMedRelasjonOgGradering(søkerFnr, ADRESSEBESKYTTELSEGRADERING.UGRADERT)
        fakeIntegrasjonKlient.leggTilBehandlendeEnhet(
            søkerFnr,
            listOf(IKKE_FORTROLIG_ENHET),
        )

        val fagsak = fagsakService.hentEllerOpprettFagsak(søkerFnr)
        val behandling =
            stegService.håndterNyBehandling(
                lagNyBehandling(fagsak.id, søkerFnr),
            )

        val arbeidsfordelingPåBehandling =
            arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id)
        assertEquals(IKKE_FORTROLIG_ENHET.enhetsnummer, arbeidsfordelingPåBehandling.behandlendeEnhetId)

        arbeidsfordelingService.manueltOppdaterBehandlendeEnhet(
            behandling,
            EndreBehandlendeEnhetDto(
                enhetId = MANUELT_OVERSTYRT_ENHET.enhetsnummer,
                begrunnelse = "",
            ),
        )

        // Act
        stegService.håndterSøknad(
            behandling,
            RegistrerSøknadDto(
                søknad =
                    lagSøknadDTO(
                        søkerFnr,
                        listOf(ugradertBarnFnr),
                    ),
                bekreftEndringerViaFrontend = false,
            ),
        )

        // Assert
        val arbeidsfordelingPåBehandlingEtterSøknadsregistrering =
            arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id)
        assertEquals(MANUELT_OVERSTYRT_ENHET.enhetsnummer, arbeidsfordelingPåBehandlingEtterSøknadsregistrering.behandlendeEnhetId)
    }

    @Test
    fun `Skal fastsette ny behandlende enhet og oppdatere eksisterende oppgave ved registrering av søknad`() {
        // Arrange
        val søkerFnr = mockSøker()
        val fortroligBarnFnr = mockBarnMedRelasjonOgGradering(søkerFnr, ADRESSEBESKYTTELSEGRADERING.FORTROLIG)
        fakeIntegrasjonKlient.leggTilBehandlendeEnhet(
            søkerFnr,
            listOf(IKKE_FORTROLIG_ENHET),
        )
        fakeIntegrasjonKlient.leggTilBehandlendeEnhet(
            fortroligBarnFnr,
            listOf(FORTROLIG_ENHET),
        )

        val fagsak = fagsakService.hentEllerOpprettFagsak(søkerFnr)
        val behandling =
            stegService.håndterNyBehandling(
                lagNyBehandling(fagsak.id, søkerFnr),
            )

        val arbeidsfordelingPåBehandling =
            arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id)
        assertEquals(IKKE_FORTROLIG_ENHET.enhetsnummer, arbeidsfordelingPåBehandling.behandlendeEnhetId)

        oppgaveService.opprettOppgave(behandling.id, Oppgavetype.BehandleSak, now())

        // Act
        stegService.håndterSøknad(
            behandling,
            RegistrerSøknadDto(
                søknad =
                    lagSøknadDTO(
                        søkerFnr,
                        listOf(fortroligBarnFnr),
                    ),
                bekreftEndringerViaFrontend = false,
            ),
        )

        val arbeidsfordelingPåBehandlingEtterSøknadsregistreringUtenDiskresjonskode =
            arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id)
        assertEquals(
            FORTROLIG_ENHET.enhetsnummer,
            arbeidsfordelingPåBehandlingEtterSøknadsregistreringUtenDiskresjonskode.behandlendeEnhetId,
        )
    }

    private fun lagNyBehandling(
        fagsakId: Long,
        søkerFnr: String,
    ) = NyBehandling(
        kategori = BehandlingKategori.NASJONAL,
        underkategori = BehandlingUnderkategori.ORDINÆR,
        søkersIdent = søkerFnr,
        behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
        søknadMottattDato = now(),
        fagsakId = fagsakId,
    )

    private fun mockSøker(): String =
        leggTilPersonInfo(
            randomSøkerFødselsdato(),
            PersonInfo(
                fødselsdato = now().minusYears(20),
                navn = "Søker Mockesen",
                kjønn = Kjønn.KVINNE,
                sivilstander = listOf(Sivilstand(type = SIVILSTANDTYPE.UGIFT)),
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                bostedsadresser = søkerBostedsadresse,
            ),
        )

    private fun mockBarnMedRelasjonOgGradering(
        relasjonFnr: String,
        adressebeskyttelseGradering: ADRESSEBESKYTTELSEGRADERING,
    ): String {
        val barnFnr =
            leggTilPersonInfo(
                randomBarnFødselsdato(),
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

    companion object {
        val MANUELT_OVERSTYRT_ENHET = BarnetrygdEnhet.OSLO
        val IKKE_FORTROLIG_ENHET = BarnetrygdEnhet.DRAMMEN
        val FORTROLIG_ENHET = BarnetrygdEnhet.VIKAFOSSEN
        val søkerBostedsadresse = listOf(lagBostedsadresse())
    }
}
