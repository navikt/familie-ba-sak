package no.nav.familie.ba.sak.kjerne.arbeidsfordeling

import io.mockk.Called
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagPersonEnkel
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
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
        )

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
                    enhetId = BarnetrygdEnhet.MIDLERTIDIG_ENHET.enhetsnummer,
                    enhetNavn = BarnetrygdEnhet.MIDLERTIDIG_ENHET.enhetsnavn,
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
            } returns Arbeidsfordelingsenhet(enhetId = BarnetrygdEnhet.OSLO.enhetsnummer, enhetNavn = BarnetrygdEnhet.OSLO.enhetsnavn)

            val arbeidsfordelingPåBehandlingSlot = slot<ArbeidsfordelingPåBehandling>()

            every {
                arbeidsfordelingPåBehandlingRepository.save(capture(arbeidsfordelingPåBehandlingSlot))
            } returnsArgument 0

            // Act
            arbeidsfordelingService.fastsettBehandlendeEnhet(behandling, null)

            // Assert
            val arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandlingSlot.captured
            assertThat(arbeidsfordelingPåBehandling.behandlendeEnhetId).isEqualTo(BarnetrygdEnhet.OSLO.enhetsnummer)
            assertThat(arbeidsfordelingPåBehandling.behandlendeEnhetNavn).isEqualTo(BarnetrygdEnhet.OSLO.enhetsnavn)
        }

        @ParameterizedTest
        @EnumSource(BehandlingÅrsak::class, names = ["SATSENDRING", "MÅNEDLIG_VALUTAJUSTERING", "SMÅBARNSTILLEGG", "OMREGNING_18ÅR", "OMREGNING_SMÅBARNSTILLEGG"], mode = EnumSource.Mode.INCLUDE)
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
        @EnumSource(BehandlingÅrsak::class, names = ["SATSENDRING", "MÅNEDLIG_VALUTAJUSTERING", "SMÅBARNSTILLEGG", "OMREGNING_18ÅR", "OMREGNING_SMÅBARNSTILLEGG"], mode = EnumSource.Mode.INCLUDE)
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
            assertThat(arbeidsfordelingPåBehandling.behandlendeEnhetId).isEqualTo(BarnetrygdEnhet.MIDLERTIDIG_ENHET.enhetsnummer)
            assertThat(arbeidsfordelingPåBehandling.behandlendeEnhetNavn).isEqualTo(BarnetrygdEnhet.MIDLERTIDIG_ENHET.enhetsnavn)
            assertThat(arbeidsfordelingPåBehandling.behandlingId).isEqualTo(behandling.id)
            assertThat(arbeidsfordelingPåBehandling.id).isEqualTo(0)
        }

        @ParameterizedTest
        @EnumSource(BehandlingÅrsak::class, names = ["SATSENDRING", "MÅNEDLIG_VALUTAJUSTERING", "SMÅBARNSTILLEGG", "OMREGNING_18ÅR", "OMREGNING_SMÅBARNSTILLEGG"], mode = EnumSource.Mode.INCLUDE)
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
            } returns ArbeidsfordelingPåBehandling(behandlingId = forrigeBehandling.id, behandlendeEnhetId = BarnetrygdEnhet.OSLO.enhetsnummer, behandlendeEnhetNavn = BarnetrygdEnhet.OSLO.enhetsnavn)

            every { arbeidsfordelingPåBehandlingRepository.save(capture(arbeidsfordelingPåBehandlingSlot)) } answers { firstArg() }

            // Act
            arbeidsfordelingService.fastsettBehandlendeEnhet(behandling, forrigeBehandling)

            // Assert
            val arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandlingSlot.captured
            assertThat(arbeidsfordelingPåBehandling.behandlendeEnhetId).isEqualTo(BarnetrygdEnhet.OSLO.enhetsnummer)
            assertThat(arbeidsfordelingPåBehandling.behandlendeEnhetNavn).isEqualTo(BarnetrygdEnhet.OSLO.enhetsnavn)
            assertThat(arbeidsfordelingPåBehandling.behandlingId).isEqualTo(behandling.id)
            assertThat(arbeidsfordelingPåBehandling.id).isEqualTo(0)
        }

        @ParameterizedTest
        @EnumSource(BehandlingÅrsak::class, names = ["SATSENDRING", "MÅNEDLIG_VALUTAJUSTERING", "SMÅBARNSTILLEGG", "OMREGNING_18ÅR", "OMREGNING_SMÅBARNSTILLEGG"], mode = EnumSource.Mode.INCLUDE)
        fun `fastsettBehandlendeEnhet skal ikke gjøre noe dersom aktiv behandlende enhet finnes`(behandlingÅrsak: BehandlingÅrsak) {
            // Arrange
            val forrigeBehandling = lagBehandling()
            val behandling = lagBehandling(årsak = behandlingÅrsak)

            every {
                arbeidsfordelingPåBehandlingRepository.finnArbeidsfordelingPåBehandling(any())
            } returns ArbeidsfordelingPåBehandling(behandlingId = forrigeBehandling.id, behandlendeEnhetId = BarnetrygdEnhet.OSLO.enhetsnummer, behandlendeEnhetNavn = BarnetrygdEnhet.OSLO.enhetsnavn)

            // Act
            arbeidsfordelingService.fastsettBehandlendeEnhet(behandling, forrigeBehandling)

            // Assert
            verify(exactly = 0) { loggService.opprettBehandlendeEnhetEndret(any(), any(), any(), any(), any()) }
            verify(exactly = 0) { oppgaveService.endreTilordnetEnhetPåOppgaverForBehandling(any(), any()) }
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
                    behandlendeEnhetId = BarnetrygdEnhet.OSLO.enhetsnummer,
                    id = 0,
                    behandlingId = behandling.id,
                    behandlendeEnhetNavn = BarnetrygdEnhet.OSLO.enhetsnavn,
                )

            every {
                arbeidsfordelingPåBehandlingRepository.finnArbeidsfordelingPåBehandling(any())
            } returns nåværendeArbeidsfordelingsenhetPåBehandling

            // Act
            arbeidsfordelingService.oppdaterBehandlendeEnhetPåBehandlingIForbindelseMedPorteføljejustering(
                behandling = behandling,
                nyEnhetId = BarnetrygdEnhet.OSLO.enhetsnummer,
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
                    behandlendeEnhetId = BarnetrygdEnhet.STEINKJER.enhetsnummer,
                    id = 0,
                    behandlingId = behandling.id,
                    behandlendeEnhetNavn = BarnetrygdEnhet.STEINKJER.enhetsnavn,
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
                nyEnhetId = BarnetrygdEnhet.OSLO.enhetsnummer,
            )

            // Assert
            val lagretArbeidsfordelingPåBehandling = lagretArbeidsfordelingPåBehandlingSlot.captured

            assertThat(lagretArbeidsfordelingPåBehandling.behandlendeEnhetId).isEqualTo(BarnetrygdEnhet.OSLO.enhetsnummer)
            assertThat(lagretArbeidsfordelingPåBehandling.behandlendeEnhetNavn).isEqualTo(BarnetrygdEnhet.OSLO.enhetsnavn)

            verify(exactly = 1) { arbeidsfordelingPåBehandlingRepository.save(any()) }
            verify(exactly = 1) {
                loggService.opprettBehandlendeEnhetEndret(
                    behandling,
                    fraEnhet = Arbeidsfordelingsenhet(BarnetrygdEnhet.STEINKJER.enhetsnummer, BarnetrygdEnhet.STEINKJER.enhetsnavn),
                    tilEnhet = lagretArbeidsfordelingPåBehandling,
                    manuellOppdatering = false,
                    begrunnelse = "Porteføljejustering",
                )
            }
            verify(exactly = 1) { saksstatistikkEventPublisher.publiserBehandlingsstatistikk(behandling.id) }
        }
    }
}
