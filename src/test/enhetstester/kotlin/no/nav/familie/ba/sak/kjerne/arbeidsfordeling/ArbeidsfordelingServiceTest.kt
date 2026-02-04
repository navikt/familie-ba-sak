package no.nav.familie.ba.sak.kjerne.arbeidsfordeling

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.datagenerator.lagArbeidsfordelingPåBehandling
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagPersonEnkel
import no.nav.familie.ba.sak.datagenerator.lagPersonInfo
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.FalskIdentitetPersonInfo
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlPersonInfo
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.BarnetrygdEnhet.MIDLERTIDIG_ENHET
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.BarnetrygdEnhet.OSLO
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.BarnetrygdEnhet.STEINKJER
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.statistikk.saksstatistikk.SaksstatistikkEventPublisher
import no.nav.familie.kontrakter.felles.NavIdent
import no.nav.familie.kontrakter.felles.oppgave.Behandlingstype
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class ArbeidsfordelingServiceTest {
    private val arbeidsfordelingPåBehandlingRepository: ArbeidsfordelingPåBehandlingRepository = mockk()
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository = mockk()
    private val personidentService: PersonidentService = mockk()
    private val oppgaveService: OppgaveService = mockk()
    private val loggService: LoggService = mockk()
    private val integrasjonKlient: IntegrasjonKlient = mockk()
    private val personopplysningerService: PersonopplysningerService = mockk()
    private val saksstatistikkEventPublisher: SaksstatistikkEventPublisher = mockk()
    private val tilpassArbeidsfordelingService: TilpassArbeidsfordelingService = mockk()
    private val featureToggleService: FeatureToggleService = mockk()

    private val arbeidsfordelingService: ArbeidsfordelingService =
        ArbeidsfordelingService(
            arbeidsfordelingPåBehandlingRepository = arbeidsfordelingPåBehandlingRepository,
            personopplysningGrunnlagRepository = personopplysningGrunnlagRepository,
            personidentService = personidentService,
            oppgaveService = oppgaveService,
            loggService = loggService,
            integrasjonKlient = integrasjonKlient,
            personopplysningerService = personopplysningerService,
            saksstatistikkEventPublisher = saksstatistikkEventPublisher,
            tilpassArbeidsfordelingService = tilpassArbeidsfordelingService,
            featureToggleService = featureToggleService,
        )

    @Nested
    inner class FastsettBehandlendeEnhet {
        @BeforeEach
        fun setup() {
            every { personopplysningGrunnlagRepository.finnSøkerOgBarnAktørerTilAktiv(any()) } returns emptyList()
            every { personopplysningerService.hentPdlPersonInfoEnkel(any()) } returns PdlPersonInfo.Person(lagPersonInfo())
            every { arbeidsfordelingPåBehandlingRepository.finnArbeidsfordelingPåBehandling(any()) } returns null
        }

        @Test
        fun `skal overstyre behandlende enhet fra NORG dersom enhet fra finnArbeidsfordelingForOppgave er en annen`() {
            // Arrange
            val behandling = lagBehandling()
            val søker = lagPersonEnkel(PersonType.SØKER, behandling.fagsak.aktør)
            val arbeidsfordelingsenhet =
                Arbeidsfordelingsenhet(
                    enhetId = MIDLERTIDIG_ENHET.enhetsnummer,
                    enhetNavn = MIDLERTIDIG_ENHET.enhetsnavn,
                )

            every { integrasjonKlient.hentBehandlendeEnhet(søker.aktør.aktivFødselsnummer(), any()) } returns
                listOf(arbeidsfordelingsenhet)

            every {
                tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(
                    arbeidsfordelingsenhet = arbeidsfordelingsenhet,
                    navIdent = NavIdent(SikkerhetContext.hentSaksbehandler()),
                )
            } returns Arbeidsfordelingsenhet(enhetId = OSLO.enhetsnummer, enhetNavn = OSLO.enhetsnavn)

            val arbeidsfordelingPåBehandlingSlot = slot<ArbeidsfordelingPåBehandling>()

            every {
                arbeidsfordelingPåBehandlingRepository.save(capture(arbeidsfordelingPåBehandlingSlot))
            } returnsArgument 0

            // Act
            arbeidsfordelingService.fastsettBehandlendeEnhet(behandling, null)

            // Assert
            val arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandlingSlot.captured
            assertThat(arbeidsfordelingPåBehandling.behandlendeEnhetId).isEqualTo(OSLO.enhetsnummer)
            assertThat(arbeidsfordelingPåBehandling.behandlendeEnhetNavn).isEqualTo(OSLO.enhetsnavn)
        }

        @ParameterizedTest
        @EnumSource(BehandlingÅrsak::class, names = ["SATSENDRING", "MÅNEDLIG_VALUTAJUSTERING", "SMÅBARNSTILLEGG", "OMREGNING_18ÅR", "OMREGNING_SMÅBARNSTILLEGG", "FINNMARKSTILLEGG", "SVALBARDTILLEGG"], mode = EnumSource.Mode.INCLUDE)
        fun `fastsettBehandlendeEnhet skal kaste Feil hvis forrige behandling er null for automatiske behandlinger som skal ha tidligere behandlinger`(behandlingÅrsak: BehandlingÅrsak) {
            // Arrange
            val behandling = lagBehandling(årsak = behandlingÅrsak)

            // Act & Assert
            val exception =
                assertThrows<Feil> {
                    arbeidsfordelingService.fastsettBehandlendeEnhet(behandling, null)
                }
            assertThat(exception.message).isEqualTo("Kan ikke fastsette arbeidsfordelingsenhet. Finner ikke tidligere behandling.")
        }

        @ParameterizedTest
        @EnumSource(BehandlingÅrsak::class, names = ["SATSENDRING", "MÅNEDLIG_VALUTAJUSTERING", "SMÅBARNSTILLEGG", "OMREGNING_18ÅR", "OMREGNING_SMÅBARNSTILLEGG", "FINNMARKSTILLEGG", "SVALBARDTILLEGG"], mode = EnumSource.Mode.INCLUDE)
        fun `fastsettBehandlendeEnhet skal sette 4863 til behandlende enhet dersom ingen av de tidligere behandlingene har hatt en annen behandlende enhet enn 4863`(behandlingÅrsak: BehandlingÅrsak) {
            // Arrange
            val forrigeBehandling = lagBehandling()
            val behandling = lagBehandling(årsak = behandlingÅrsak)

            val arbeidsfordelingPåBehandlingSlot = slot<ArbeidsfordelingPåBehandling>()

            every {
                arbeidsfordelingPåBehandlingRepository.finnSisteGyldigeArbeidsfordelingPåBehandlingIFagsak(behandling.fagsak.id)
            } returns null

            every { arbeidsfordelingPåBehandlingRepository.save(capture(arbeidsfordelingPåBehandlingSlot)) } answers { firstArg() }

            every { integrasjonKlient.hentBehandlendeEnhet(any(), any()) } returns
                listOf(
                    Arbeidsfordelingsenhet(
                        enhetId = MIDLERTIDIG_ENHET.enhetsnummer,
                        enhetNavn = MIDLERTIDIG_ENHET.enhetsnavn,
                    ),
                )

            // Act
            arbeidsfordelingService.fastsettBehandlendeEnhet(behandling, forrigeBehandling)

            // Assert
            val arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandlingSlot.captured
            assertThat(arbeidsfordelingPåBehandling.behandlendeEnhetId).isEqualTo(MIDLERTIDIG_ENHET.enhetsnummer)
            assertThat(arbeidsfordelingPåBehandling.behandlendeEnhetNavn).isEqualTo(MIDLERTIDIG_ENHET.enhetsnavn)
            assertThat(arbeidsfordelingPåBehandling.behandlingId).isEqualTo(behandling.id)
            assertThat(arbeidsfordelingPåBehandling.id).isEqualTo(0)
        }

        @ParameterizedTest
        @EnumSource(BehandlingÅrsak::class, names = ["SATSENDRING", "MÅNEDLIG_VALUTAJUSTERING", "SMÅBARNSTILLEGG", "OMREGNING_18ÅR", "OMREGNING_SMÅBARNSTILLEGG", "FINNMARKSTILLEGG", "SVALBARDTILLEGG"], mode = EnumSource.Mode.INCLUDE)
        fun `fastsettBehandlendeEnhet skal sette behandlende enhet til en gyldig enhet dersom en av de tidligere behandlingene har hatt en annen behandlende enhet enn 4863`(behandlingÅrsak: BehandlingÅrsak) {
            // Arrange
            val forrigeBehandling = lagBehandling()
            val behandling = lagBehandling(årsak = behandlingÅrsak)

            val arbeidsfordelingPåBehandlingSlot = slot<ArbeidsfordelingPåBehandling>()

            every {
                arbeidsfordelingPåBehandlingRepository.finnSisteGyldigeArbeidsfordelingPåBehandlingIFagsak(behandling.fagsak.id)
            } returns ArbeidsfordelingPåBehandling(behandlingId = forrigeBehandling.id, behandlendeEnhetId = OSLO.enhetsnummer, behandlendeEnhetNavn = OSLO.enhetsnavn)

            every { arbeidsfordelingPåBehandlingRepository.save(capture(arbeidsfordelingPåBehandlingSlot)) } answers { firstArg() }

            // Act
            arbeidsfordelingService.fastsettBehandlendeEnhet(behandling, forrigeBehandling)

            // Assert
            val arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandlingSlot.captured
            assertThat(arbeidsfordelingPåBehandling.behandlendeEnhetId).isEqualTo(OSLO.enhetsnummer)
            assertThat(arbeidsfordelingPåBehandling.behandlendeEnhetNavn).isEqualTo(OSLO.enhetsnavn)
            assertThat(arbeidsfordelingPåBehandling.behandlingId).isEqualTo(behandling.id)
            assertThat(arbeidsfordelingPåBehandling.id).isEqualTo(0)
        }

        @ParameterizedTest
        @EnumSource(BehandlingÅrsak::class, names = ["SATSENDRING", "MÅNEDLIG_VALUTAJUSTERING", "SMÅBARNSTILLEGG", "OMREGNING_18ÅR", "OMREGNING_SMÅBARNSTILLEGG", "FINNMARKSTILLEGG", "SVALBARDTILLEGG"], mode = EnumSource.Mode.INCLUDE)
        fun `fastsettBehandlendeEnhet skal ikke gjøre noe dersom aktiv behandlende enhet finnes`(behandlingÅrsak: BehandlingÅrsak) {
            // Arrange
            val forrigeBehandling = lagBehandling()
            val behandling = lagBehandling(årsak = behandlingÅrsak)

            every {
                arbeidsfordelingPåBehandlingRepository.finnArbeidsfordelingPåBehandling(any())
            } returns ArbeidsfordelingPåBehandling(behandlingId = forrigeBehandling.id, behandlendeEnhetId = OSLO.enhetsnummer, behandlendeEnhetNavn = OSLO.enhetsnavn)

            // Act
            arbeidsfordelingService.fastsettBehandlendeEnhet(behandling, forrigeBehandling)

            // Assert
            verify(exactly = 0) { loggService.opprettBehandlendeEnhetEndret(any(), any(), any(), any(), any()) }
            verify(exactly = 0) { oppgaveService.endreTilordnetEnhetPåOppgaverForBehandling(any(), any()) }
        }

        @Test
        fun `fastsettBehandlendeEnhet skal bruke behandlende enhet fra forrige behandling dersom person med sterkest adressebeskyttelse har falsk identitet`() {
            // Arrange
            val søker = lagPerson()
            val barn = lagPerson(type = PersonType.BARN)
            val fagsak = lagFagsak(aktør = søker.aktør)
            val forrigeBehandling = lagBehandling(fagsak = fagsak)
            val behandling = lagBehandling(fagsak = fagsak, behandlingType = BehandlingType.REVURDERING)

            every {
                personopplysningGrunnlagRepository
                    .finnSøkerOgBarnAktørerTilAktiv(behandling.id)
            } returns listOf(lagPersonEnkel(PersonType.SØKER, søker.aktør), lagPersonEnkel(PersonType.BARN, barn.aktør))

            every { personopplysningerService.hentPdlPersonInfoEnkel(barn.aktør) } returns PdlPersonInfo.Person(lagPersonInfo(adressebeskyttelseGradering = null))
            every { personopplysningerService.hentPdlPersonInfoEnkel(søker.aktør) } returns PdlPersonInfo.FalskPerson(FalskIdentitetPersonInfo())

            every { arbeidsfordelingPåBehandlingRepository.finnSisteGyldigeArbeidsfordelingPåBehandlingIFagsak(fagsak.id) } returns
                lagArbeidsfordelingPåBehandling(
                    behandlendeEnhetId = OSLO.enhetsnummer,
                    behandlendeEnhetNavn = OSLO.enhetsnavn,
                    behandlingId = forrigeBehandling.id,
                    manueltOverstyrt = false,
                )

            every { tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(any(), any()) } returns
                Arbeidsfordelingsenhet(
                    enhetId = OSLO.enhetsnummer,
                    enhetNavn = OSLO.enhetsnavn,
                )

            val arbeidsfordelingPåBehandlingSlot = slot<ArbeidsfordelingPåBehandling>()
            every { arbeidsfordelingPåBehandlingRepository.save(capture(arbeidsfordelingPåBehandlingSlot)) } answers { firstArg() }

            // Act
            arbeidsfordelingService.fastsettBehandlendeEnhet(behandling, forrigeBehandling)

            // Assert
            val arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandlingSlot.captured
            assertThat(arbeidsfordelingPåBehandling.behandlendeEnhetId).isEqualTo(OSLO.enhetsnummer)
            assertThat(arbeidsfordelingPåBehandling.behandlendeEnhetNavn).isEqualTo(OSLO.enhetsnavn)
        }

        @Test
        fun `fastsettBehandlendeEnhet skal hente behandlende enhet fra NORG dersom person med sterkest adressebeskyttelse ikke har falsk identitet`() {
            // Arrange
            val søker = lagPerson()
            val barn = lagPerson(type = PersonType.BARN)
            val fagsak = lagFagsak(aktør = søker.aktør)
            val forrigeBehandling = lagBehandling(fagsak = fagsak)
            val behandling = lagBehandling(fagsak = fagsak, behandlingType = BehandlingType.REVURDERING)

            every {
                arbeidsfordelingPåBehandlingRepository.finnArbeidsfordelingPåBehandling(any())
            } returns null

            every {
                personopplysningGrunnlagRepository
                    .finnSøkerOgBarnAktørerTilAktiv(behandling.id)
            } returns listOf(lagPersonEnkel(PersonType.SØKER, søker.aktør), lagPersonEnkel(PersonType.BARN, barn.aktør))

            every { personopplysningerService.hentPdlPersonInfoEnkel(barn.aktør) } returns PdlPersonInfo.Person(lagPersonInfo(adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG))
            every { personopplysningerService.hentPdlPersonInfoEnkel(søker.aktør) } returns PdlPersonInfo.FalskPerson(FalskIdentitetPersonInfo())
            every { integrasjonKlient.hentBehandlendeEnhet(barn.aktør.aktivFødselsnummer(), Behandlingstype.NASJONAL) } returns
                listOf(
                    Arbeidsfordelingsenhet(
                        enhetId = BarnetrygdEnhet.DRAMMEN.enhetsnummer,
                        enhetNavn = BarnetrygdEnhet.DRAMMEN.enhetsnavn,
                    ),
                )

            every { tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(any(), any()) } returns
                Arbeidsfordelingsenhet(
                    enhetId = BarnetrygdEnhet.DRAMMEN.enhetsnummer,
                    enhetNavn = BarnetrygdEnhet.DRAMMEN.enhetsnavn,
                )

            val arbeidsfordelingPåBehandlingSlot = slot<ArbeidsfordelingPåBehandling>()
            every { arbeidsfordelingPåBehandlingRepository.save(capture(arbeidsfordelingPåBehandlingSlot)) } answers { firstArg() }

            // Act
            arbeidsfordelingService.fastsettBehandlendeEnhet(behandling, forrigeBehandling)

            // Assert
            val arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandlingSlot.captured
            assertThat(arbeidsfordelingPåBehandling.behandlendeEnhetId).isEqualTo(BarnetrygdEnhet.DRAMMEN.enhetsnummer)
            assertThat(arbeidsfordelingPåBehandling.behandlendeEnhetNavn).isEqualTo(BarnetrygdEnhet.DRAMMEN.enhetsnavn)
        }
    }

    @Nested
    inner class FastsettBehandlendeEnhetEtterPorteføljejustering {
        @BeforeEach
        fun setup() {
            every { personopplysningGrunnlagRepository.finnSøkerOgBarnAktørerTilAktiv(any()) } returns emptyList()
            every { personopplysningerService.hentPdlPersonInfoEnkel(any()) } returns PdlPersonInfo.Person(lagPersonInfo())
            every { arbeidsfordelingPåBehandlingRepository.finnArbeidsfordelingPåBehandling(any()) } returns null
        }

        @ParameterizedTest
        @EnumSource(BehandlingÅrsak::class, names = ["SATSENDRING", "MÅNEDLIG_VALUTAJUSTERING", "SMÅBARNSTILLEGG", "OMREGNING_18ÅR", "OMREGNING_SMÅBARNSTILLEGG", "FINNMARKSTILLEGG", "SVALBARDTILLEGG"], mode = EnumSource.Mode.INCLUDE)
        fun `fastsettBehandlendeEnhet skal bruke ny enhet fra NORG hvis forrige enhet var 4817`(behandlingÅrsak: BehandlingÅrsak) {
            // Arrange
            val forrigeBehandling = lagBehandling()
            val behandling = lagBehandling(årsak = behandlingÅrsak)

            val arbeidsfordelingPåBehandlingSlot = slot<ArbeidsfordelingPåBehandling>()

            // Forrige enhet
            every { arbeidsfordelingPåBehandlingRepository.finnSisteGyldigeArbeidsfordelingPåBehandlingIFagsak(behandling.fagsak.id) } returns
                ArbeidsfordelingPåBehandling(
                    behandlingId = forrigeBehandling.id,
                    behandlendeEnhetId = STEINKJER.enhetsnummer,
                    behandlendeEnhetNavn = STEINKJER.enhetsnavn,
                )

            // Ny enhet
            every { integrasjonKlient.hentBehandlendeEnhet(behandling.fagsak.aktør.aktivFødselsnummer(), any()) } returns
                listOf(
                    Arbeidsfordelingsenhet.opprettFra(BarnetrygdEnhet.OSLO),
                )

            every { arbeidsfordelingPåBehandlingRepository.save(capture(arbeidsfordelingPåBehandlingSlot)) } answers { firstArg() }

            // Act
            arbeidsfordelingService.fastsettBehandlendeEnhet(behandling, forrigeBehandling)

            // Assert
            val arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandlingSlot.captured
            assertThat(arbeidsfordelingPåBehandling.behandlendeEnhetId).isEqualTo(OSLO.enhetsnummer)
            assertThat(arbeidsfordelingPåBehandling.behandlendeEnhetNavn).isEqualTo(OSLO.enhetsnavn)
            assertThat(arbeidsfordelingPåBehandling.behandlingId).isEqualTo(behandling.id)
            assertThat(arbeidsfordelingPåBehandling.id).isEqualTo(0)
        }

        @ParameterizedTest
        @EnumSource(BehandlingÅrsak::class, names = ["SATSENDRING", "MÅNEDLIG_VALUTAJUSTERING", "SMÅBARNSTILLEGG", "OMREGNING_18ÅR", "OMREGNING_SMÅBARNSTILLEGG", "FINNMARKSTILLEGG", "SVALBARDTILLEGG"], mode = EnumSource.Mode.INCLUDE)
        fun `fastsettBehandlendeEnhet skal bruke ny enhet fra NORG hvis forrige enhet var 4863`(behandlingÅrsak: BehandlingÅrsak) {
            // Arrange
            val forrigeBehandling = lagBehandling()
            val behandling = lagBehandling(årsak = behandlingÅrsak)

            val arbeidsfordelingPåBehandlingSlot = slot<ArbeidsfordelingPåBehandling>()

            // Forrige enhet
            every { arbeidsfordelingPåBehandlingRepository.finnSisteGyldigeArbeidsfordelingPåBehandlingIFagsak(behandling.fagsak.id) } returns
                ArbeidsfordelingPåBehandling(
                    behandlingId = forrigeBehandling.id,
                    behandlendeEnhetId = MIDLERTIDIG_ENHET.enhetsnummer,
                    behandlendeEnhetNavn = MIDLERTIDIG_ENHET.enhetsnavn,
                )

            // Ny enhet
            every { integrasjonKlient.hentBehandlendeEnhet(behandling.fagsak.aktør.aktivFødselsnummer(), any()) } returns
                listOf(
                    Arbeidsfordelingsenhet.opprettFra(BarnetrygdEnhet.OSLO),
                )

            every { arbeidsfordelingPåBehandlingRepository.save(capture(arbeidsfordelingPåBehandlingSlot)) } answers { firstArg() }

            // Act
            arbeidsfordelingService.fastsettBehandlendeEnhet(behandling, forrigeBehandling)

            // Assert
            val arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandlingSlot.captured
            assertThat(arbeidsfordelingPåBehandling.behandlendeEnhetId).isEqualTo(OSLO.enhetsnummer)
            assertThat(arbeidsfordelingPåBehandling.behandlendeEnhetNavn).isEqualTo(OSLO.enhetsnavn)
            assertThat(arbeidsfordelingPåBehandling.behandlingId).isEqualTo(behandling.id)
            assertThat(arbeidsfordelingPåBehandling.id).isEqualTo(0)
        }

        @ParameterizedTest
        @EnumSource(BehandlingÅrsak::class, names = ["SATSENDRING", "MÅNEDLIG_VALUTAJUSTERING", "SMÅBARNSTILLEGG", "OMREGNING_18ÅR", "OMREGNING_SMÅBARNSTILLEGG", "FINNMARKSTILLEGG", "SVALBARDTILLEGG"], mode = EnumSource.Mode.INCLUDE)
        fun `fastsettBehandlendeEnhet skal bruke ny enhet hvis ny enhet fra NORG er 4863`(behandlingÅrsak: BehandlingÅrsak) {
            // Arrange
            val forrigeBehandling = lagBehandling()
            val behandling = lagBehandling(årsak = behandlingÅrsak)

            val arbeidsfordelingPåBehandlingSlot = slot<ArbeidsfordelingPåBehandling>()

            // Forrige enhet
            every { arbeidsfordelingPåBehandlingRepository.finnSisteGyldigeArbeidsfordelingPåBehandlingIFagsak(behandling.fagsak.id) } returns
                ArbeidsfordelingPåBehandling(
                    behandlingId = forrigeBehandling.id,
                    behandlendeEnhetId = MIDLERTIDIG_ENHET.enhetsnummer,
                    behandlendeEnhetNavn = MIDLERTIDIG_ENHET.enhetsnavn,
                )

            // Ny enhet
            every { integrasjonKlient.hentBehandlendeEnhet(behandling.fagsak.aktør.aktivFødselsnummer(), any()) } returns
                listOf(
                    Arbeidsfordelingsenhet(
                        enhetId = MIDLERTIDIG_ENHET.enhetsnummer,
                        enhetNavn = MIDLERTIDIG_ENHET.enhetsnavn,
                    ),
                )

            every { arbeidsfordelingPåBehandlingRepository.save(capture(arbeidsfordelingPåBehandlingSlot)) } answers { firstArg() }

            // Act
            arbeidsfordelingService.fastsettBehandlendeEnhet(behandling, forrigeBehandling)

            // Assert
            val arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandlingSlot.captured
            assertThat(arbeidsfordelingPåBehandling.behandlendeEnhetId).isEqualTo(MIDLERTIDIG_ENHET.enhetsnummer)
            assertThat(arbeidsfordelingPåBehandling.behandlendeEnhetNavn).isEqualTo(MIDLERTIDIG_ENHET.enhetsnavn)
            assertThat(arbeidsfordelingPåBehandling.behandlingId).isEqualTo(behandling.id)
            assertThat(arbeidsfordelingPåBehandling.id).isEqualTo(0)
        }
    }

    @Nested
    inner class ManueltOppdaterBehandlendeEnhetTest {
        @Test
        fun `Skal kaste feil ved forsøk på å endre behandlende enhet til Steinkjer`() {
            // Arrange
            val behandling = lagBehandling()

            val endreBehandlendeEnhet =
                EndreBehandlendeEnhetDto(
                    enhetId = STEINKJER.enhetsnummer,
                    begrunnelse = "Begrunnelse for endring",
                )

            val feilmelding =
                assertThrows<FunksjonellFeil> {
                    arbeidsfordelingService.manueltOppdaterBehandlendeEnhet(
                        behandling = behandling,
                        endreBehandlendeEnhet = endreBehandlendeEnhet,
                    )
                }.melding

            assertThat(feilmelding).isEqualTo("Fra og med 5. januar 2026 er det ikke lenger å mulig å endre behandlende enhet til Steinkjer.")
        }
    }
}
