package no.nav.familie.ba.sak.kjerne.arbeidsfordeling

import io.mockk.Called
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle.HENT_ARBEIDSFORDELING_FOR_AUTOMATISK_BEHANDLING_ETTER_PORTEFØLJEJUSTERING
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagPersonEnkel
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.BarnetrygdEnhet.MIDLERTIDIG_ENHET
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.BarnetrygdEnhet.OSLO
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.BarnetrygdEnhet.STEINKJER
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.statistikk.saksstatistikk.SaksstatistikkEventPublisher
import no.nav.familie.kontrakter.felles.NavIdent
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

    @BeforeEach
    fun setup() {
        every { featureToggleService.isEnabled(HENT_ARBEIDSFORDELING_FOR_AUTOMATISK_BEHANDLING_ETTER_PORTEFØLJEJUSTERING) } returns false
    }

    @Nested
    inner class FastsettBehandlendeEnhet {
        @Test
        fun `skal overstyre behandlende enhet fra NORG dersom enhet fra finnArbeidsfordelingForOppgave er en annen`() {
            // Arrange
            val behandling = lagBehandling()
            val søker = lagPersonEnkel(PersonType.SØKER, behandling.fagsak.aktør)
            val barn = lagPersonEnkel(PersonType.BARN)
            val arbeidsfordelingsenhet =
                Arbeidsfordelingsenhet(
                    enhetId = MIDLERTIDIG_ENHET.enhetsnummer,
                    enhetNavn = MIDLERTIDIG_ENHET.enhetsnavn,
                )

            every {
                arbeidsfordelingPåBehandlingRepository.finnArbeidsfordelingPåBehandling(behandling.id)
            } returns null

            every { personopplysningerService.hentPersoninfoEnkel(any()).adressebeskyttelseGradering } returns null

            every {
                personopplysningGrunnlagRepository
                    .finnSøkerOgBarnAktørerTilAktiv(behandling.id)
            } returns listOf(søker, barn)

            every { integrasjonKlient.hentBehandlendeEnhet(søker.aktør.aktivFødselsnummer()) } returns
                listOf(
                    arbeidsfordelingsenhet,
                )

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

            every {
                arbeidsfordelingPåBehandlingRepository.finnArbeidsfordelingPåBehandling(any())
            } returns null

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
                arbeidsfordelingPåBehandlingRepository.finnArbeidsfordelingPåBehandling(any())
            } returns null

            every {
                arbeidsfordelingPåBehandlingRepository.finnSisteGyldigeArbeidsfordelingPåBehandlingIFagsak(behandling.fagsak.id)
            } returns null

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

        @ParameterizedTest
        @EnumSource(BehandlingÅrsak::class, names = ["SATSENDRING", "MÅNEDLIG_VALUTAJUSTERING", "SMÅBARNSTILLEGG", "OMREGNING_18ÅR", "OMREGNING_SMÅBARNSTILLEGG", "FINNMARKSTILLEGG", "SVALBARDTILLEGG"], mode = EnumSource.Mode.INCLUDE)
        fun `fastsettBehandlendeEnhet skal sette behandlende enhet til en gyldig enhet dersom en av de tidligere behandlingene har hatt en annen behandlende enhet enn 4863`(behandlingÅrsak: BehandlingÅrsak) {
            // Arrange
            val forrigeBehandling = lagBehandling()
            val behandling = lagBehandling(årsak = behandlingÅrsak)

            val arbeidsfordelingPåBehandlingSlot = slot<ArbeidsfordelingPåBehandling>()

            every {
                arbeidsfordelingPåBehandlingRepository.finnArbeidsfordelingPåBehandling(any())
            } returns null

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
    }

    @Nested
    inner class FastsettBehandlendeEnhetEtterPorteføljejustering {
        @BeforeEach
        fun setup() {
            every { featureToggleService.isEnabled(HENT_ARBEIDSFORDELING_FOR_AUTOMATISK_BEHANDLING_ETTER_PORTEFØLJEJUSTERING) } returns true
            every { personopplysningGrunnlagRepository.finnSøkerOgBarnAktørerTilAktiv(any()) } returns emptyList()
            every { personopplysningerService.hentPersoninfoEnkel(any()).adressebeskyttelseGradering } returns null
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
            every { integrasjonKlient.hentBehandlendeEnhet(behandling.fagsak.aktør.aktivFødselsnummer()) } returns
                listOf(
                    Arbeidsfordelingsenhet(
                        enhetId = OSLO.enhetsnummer,
                        enhetNavn = OSLO.enhetsnavn,
                    ),
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
            every { integrasjonKlient.hentBehandlendeEnhet(behandling.fagsak.aktør.aktivFødselsnummer()) } returns
                listOf(
                    Arbeidsfordelingsenhet(
                        enhetId = OSLO.enhetsnummer,
                        enhetNavn = OSLO.enhetsnavn,
                    ),
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
            every { integrasjonKlient.hentBehandlendeEnhet(behandling.fagsak.aktør.aktivFødselsnummer()) } returns
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

        @ParameterizedTest
        @EnumSource(BehandlingÅrsak::class, names = ["SATSENDRING", "MÅNEDLIG_VALUTAJUSTERING", "SMÅBARNSTILLEGG", "OMREGNING_18ÅR", "OMREGNING_SMÅBARNSTILLEGG", "FINNMARKSTILLEGG", "SVALBARDTILLEGG"], mode = EnumSource.Mode.INCLUDE)
        fun `fastsettBehandlendeEnhet skal kaste feil hvis ny enhet fra NORG er 4817`(behandlingÅrsak: BehandlingÅrsak) {
            // Arrange
            val forrigeBehandling = lagBehandling()
            val behandling = lagBehandling(årsak = behandlingÅrsak)

            // Forrige enhet
            every { arbeidsfordelingPåBehandlingRepository.finnSisteGyldigeArbeidsfordelingPåBehandlingIFagsak(behandling.fagsak.id) } returns
                ArbeidsfordelingPåBehandling(
                    behandlingId = forrigeBehandling.id,
                    behandlendeEnhetId = STEINKJER.enhetsnummer,
                    behandlendeEnhetNavn = STEINKJER.enhetsnavn,
                )

            // Ny enhet
            every { integrasjonKlient.hentBehandlendeEnhet(behandling.fagsak.aktør.aktivFødselsnummer()) } returns
                listOf(
                    Arbeidsfordelingsenhet(
                        enhetId = STEINKJER.enhetsnummer,
                        enhetNavn = STEINKJER.enhetsnavn,
                    ),
                )

            every { arbeidsfordelingPåBehandlingRepository.save(any()) } returns mockk()

            // Act
            val feil =
                assertThrows<Feil> {
                    arbeidsfordelingService.fastsettBehandlendeEnhet(behandling, forrigeBehandling)
                }

            // Assert
            assertThat(feil.message).isEqualTo("Kan ikke sette behandlende enhet til 'Nav familie- og pensjonsytelser Steinkjer' etter porteføljejustering.")
        }
    }

    @Nested
    inner class OppdaterBehandlendeEnhetPåBehandlingIForbindelseMedPorteføljejusteringTest {
        @Test
        fun `Skal ikke oppdatere enhet hvis ny enhet er det samme som gamle`() {
            // Arrange
            val behandling = lagBehandling()
            val nåværendeArbeidsfordelingsenhetPåBehandling =
                ArbeidsfordelingPåBehandling(
                    behandlendeEnhetId = OSLO.enhetsnummer,
                    id = 0,
                    behandlingId = behandling.id,
                    behandlendeEnhetNavn = OSLO.enhetsnavn,
                )

            every {
                arbeidsfordelingPåBehandlingRepository.finnArbeidsfordelingPåBehandling(any())
            } returns nåværendeArbeidsfordelingsenhetPåBehandling

            // Act
            arbeidsfordelingService.oppdaterBehandlendeEnhetPåBehandlingIForbindelseMedPorteføljejustering(
                behandling = behandling,
                nyEnhetId = OSLO.enhetsnummer,
            )

            // Assert
            verify(exactly = 0) { arbeidsfordelingPåBehandlingRepository.save(any()) }
            verify { loggService wasNot Called }
            verify { saksstatistikkEventPublisher wasNot Called }
        }

        @Test
        fun `Skal oppdatere enhet, opprette logg, og publisere sakstatistikk hvis ny enhet er ulikt gammel`() {
            // Arrange
            val behandling = lagBehandling()
            val nåværendeArbeidsfordelingsenhetPåBehandling =
                ArbeidsfordelingPåBehandling(
                    behandlendeEnhetId = STEINKJER.enhetsnummer,
                    id = 0,
                    behandlingId = behandling.id,
                    behandlendeEnhetNavn = STEINKJER.enhetsnavn,
                )

            every {
                arbeidsfordelingPåBehandlingRepository.finnArbeidsfordelingPåBehandling(any())
            } returns nåværendeArbeidsfordelingsenhetPåBehandling

            val lagretArbeidsfordelingPåBehandlingSlot = slot<ArbeidsfordelingPåBehandling>()
            every { arbeidsfordelingPåBehandlingRepository.save(capture(lagretArbeidsfordelingPåBehandlingSlot)) } returnsArgument 0

            every {
                loggService.opprettBehandlendeEnhetEndret(
                    behandling,
                    fraEnhet = any(),
                    tilEnhet = any(),
                    manuellOppdatering = false,
                    begrunnelse = "Porteføljejustering",
                )
            } just runs

            every { saksstatistikkEventPublisher.publiserBehandlingsstatistikk(behandling.id) } just runs

            // Act
            arbeidsfordelingService.oppdaterBehandlendeEnhetPåBehandlingIForbindelseMedPorteføljejustering(
                behandling = behandling,
                nyEnhetId = OSLO.enhetsnummer,
            )

            // Assert
            val lagretArbeidsfordelingPåBehandling = lagretArbeidsfordelingPåBehandlingSlot.captured

            assertThat(lagretArbeidsfordelingPåBehandling.behandlendeEnhetId).isEqualTo(OSLO.enhetsnummer)
            assertThat(lagretArbeidsfordelingPåBehandling.behandlendeEnhetNavn).isEqualTo(OSLO.enhetsnavn)

            verify(exactly = 1) { arbeidsfordelingPåBehandlingRepository.save(any()) }
            verify(exactly = 1) {
                loggService.opprettBehandlendeEnhetEndret(
                    behandling,
                    fraEnhet = Arbeidsfordelingsenhet(STEINKJER.enhetsnummer, STEINKJER.enhetsnavn),
                    tilEnhet = lagretArbeidsfordelingPåBehandling,
                    manuellOppdatering = false,
                    begrunnelse = "Porteføljejustering",
                )
            }
            verify(exactly = 1) { saksstatistikkEventPublisher.publiserBehandlingsstatistikk(behandling.id) }
        }
    }

    @Nested
    inner class ManueltOppdaterBehandlendeEnhetTest {
        @Test
        fun `Skal kaste feil ved forsøk på å endre behandlende enhet til Steinkjer`() {
            // Arrange
            val behandling = lagBehandling()

            val endreBehandlendeEnhet =
                RestEndreBehandlendeEnhet(
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
