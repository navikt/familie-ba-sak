package no.nav.familie.ba.sak.kjerne.autovedtak

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.datagenerator.defaultFagsak
import no.nav.familie.ba.sak.datagenerator.lagAktør
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.autovedtak.finnmarkstillegg.AutovedtakFinnmarkstilleggService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.AutovedtakFødselshendelseService
import no.nav.familie.ba.sak.kjerne.autovedtak.omregning.AutovedtakBrevService
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendringeøs.AutovedtakSatsendringEøsService
import no.nav.familie.ba.sak.kjerne.autovedtak.småbarnstillegg.AutovedtakSmåbarnstilleggService
import no.nav.familie.ba.sak.kjerne.autovedtak.svalbardtillegg.AutovedtakSvalbardtilleggService
import no.nav.familie.ba.sak.kjerne.autovedtak.søknad.AutovedtakSøknadService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.SettPåMaskinellVentÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.SnikeIKøenService
import no.nav.familie.ba.sak.kjerne.behandling.Søknad
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.task.dto.ManuellOppgaveType
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.error.RekjørSenereException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

class AutovedtakStegServiceTest {
    private val fagsakService = mockk<FagsakService>()
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val oppgaveService = mockk<OppgaveService>(relaxed = true)
    private val autovedtakFødselshendelseService = mockk<AutovedtakFødselshendelseService>()
    private val autovedtakBrevService = mockk<AutovedtakBrevService>()
    private val autovedtakSmåbarnstilleggService = mockk<AutovedtakSmåbarnstilleggService>()
    private val autovedtakFinnmarkstilleggService = mockk<AutovedtakFinnmarkstilleggService>()
    private val autovedtakSvalbardtilleggService = mockk<AutovedtakSvalbardtilleggService>()
    private val autovedtakSatsendringEøsService = mockk<AutovedtakSatsendringEøsService>()
    private val autovedtakSøknadService = mockk<AutovedtakSøknadService>()
    private val snikeIKøenService = mockk<SnikeIKøenService>()
    private val featureToggleService = mockk<FeatureToggleService>()

    private val autovedtakStegService =
        AutovedtakStegService(
            fagsakService = fagsakService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            oppgaveService = oppgaveService,
            autovedtakFødselshendelseService = autovedtakFødselshendelseService,
            autovedtakBrevService = autovedtakBrevService,
            autovedtakSmåbarnstilleggService = autovedtakSmåbarnstilleggService,
            autovedtakFinnmarkstilleggService = autovedtakFinnmarkstilleggService,
            autovedtakSvalbardtilleggService = autovedtakSvalbardtilleggService,
            autovedtakSatsendringEøsService = autovedtakSatsendringEøsService,
            autovedtakSøknadService = autovedtakSøknadService,
            snikeIKøenService = snikeIKøenService,
            featureToggleService = featureToggleService,
        )

    private val fagsak = defaultFagsak()
    private val mottakersAktør = lagAktør(randomFnr())

    private val søknad =
        Søknad(
            fagsakId = fagsak.id,
            søkersIdent = mottakersAktør.aktivFødselsnummer(),
            barnasIdenter = listOf(randomFnr()),
        )

    private val nyBehandlingHendelse =
        NyBehandlingHendelse(
            morsIdent = mottakersAktør.aktivFødselsnummer(),
            barnasIdenter = listOf(randomFnr()),
        )

    @Nested
    inner class KjørAutomatiskBehandlingSøknad {
        @BeforeEach
        fun setUp() {
            every { featureToggleService.isEnabled(FeatureToggle.SKAL_BEHANDLE_SOKNAD_AUTOMATISK) } returns true
            every { autovedtakSøknadService.skalAutovedtakBehandles(SøknadData(søknad)) } returns true
            every { fagsakService.hentPåFagsakId(fagsak.id) } returns fagsak
            every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsak.id) } returns null
            every { autovedtakSøknadService.kjørBehandling(SøknadData(søknad)) } returns "Søknad: Behandling ferdig"
        }

        @Test
        fun `skal kaste Feil når toggle SKAL_BEHANDLE_SOKNAD_AUTOMATISK er skrudd av`() {
            // Arrange
            every { featureToggleService.isEnabled(FeatureToggle.SKAL_BEHANDLE_SOKNAD_AUTOMATISK) } returns false

            // Act & Assert
            assertThrows<Feil> {
                autovedtakStegService.kjørAutomatiskBehandlingSøknad(mottakersAktør, søknad)
            }
            verify(exactly = 0) { fagsakService.hentPåFagsakId(any()) }
            verify(exactly = 0) { autovedtakSøknadService.kjørBehandling(any()) }
        }

        @Test
        fun `skal returnere tidlig uten å hente fagsak når autovedtak ikke skal behandles`() {
            // Arrange
            every { autovedtakSøknadService.skalAutovedtakBehandles(SøknadData(søknad)) } returns false

            // Act
            val resultat = autovedtakStegService.kjørAutomatiskBehandlingSøknad(mottakersAktør, søknad)

            // Assert
            assertThat(resultat).isEqualTo("Søknad: Skal ikke behandles")
            verify(exactly = 0) { fagsakService.hentPåFagsakId(any()) }
            verify(exactly = 0) { autovedtakSøknadService.kjørBehandling(any()) }
        }

        @Test
        fun `skal kjøre behandling og returnere resultat når det ikke finnes noen åpen behandling`() {
            // Act
            val resultat = autovedtakStegService.kjørAutomatiskBehandlingSøknad(mottakersAktør, søknad)

            // Assert
            assertThat(resultat).isEqualTo("Søknad: Behandling ferdig")
            verify(exactly = 1) { autovedtakSøknadService.kjørBehandling(SøknadData(søknad)) }
        }

        @Test
        fun `skal sette åpen behandling på maskinell vent og likevel kjøre ny behandling når man kan snike i køen`() {
            // Arrange
            val åpenBehandling = lagBehandling(fagsak = fagsak, status = BehandlingStatus.UTREDES)
            every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsak.id) } returns åpenBehandling
            every { snikeIKøenService.kanSnikeForbi(åpenBehandling) } returns true
            every { snikeIKøenService.settAktivBehandlingPåMaskinellVent(any(), any()) } just Runs

            // Act
            val resultat = autovedtakStegService.kjørAutomatiskBehandlingSøknad(mottakersAktør, søknad)

            // Assert
            assertThat(resultat).isEqualTo("Søknad: Behandling ferdig")
            verify(exactly = 1) {
                snikeIKøenService.settAktivBehandlingPåMaskinellVent(åpenBehandling.id, SettPåMaskinellVentÅrsak.SØKNAD)
                autovedtakSøknadService.kjørBehandling(SøknadData(søknad))
            }
        }

        @Test
        fun `skal opprette oppgave og returnere at bruker har åpen behandling når man ikke kan snike i køen`() {
            // Arrange
            val åpenBehandling = lagBehandling(fagsak = fagsak, status = BehandlingStatus.UTREDES)
            every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsak.id) } returns åpenBehandling
            every { snikeIKøenService.kanSnikeForbi(åpenBehandling) } returns false

            // Act
            val resultat = autovedtakStegService.kjørAutomatiskBehandlingSøknad(mottakersAktør, søknad)

            // Assert
            assertThat(resultat).isEqualTo("Søknad: Bruker har åpen behandling")
            verify(exactly = 0) { autovedtakSøknadService.kjørBehandling(any()) }
            verify(exactly = 1) {
                oppgaveService.opprettOppgaveForManuellBehandling(
                    behandlingId = åpenBehandling.id,
                    begrunnelse = "Søknad: Bruker har åpen behandling",
                    manuellOppgaveType = ManuellOppgaveType.ÅPEN_BEHANDLING,
                    oppgavetype = Oppgavetype.BehandleSak,
                )
            }
        }

        @Test
        fun `skal kaste RekjørSenereException når åpen behandling har status FATTER_VEDTAK og ble endret for under 7 dager siden`() {
            // Arrange
            val åpenBehandling = lagBehandling(fagsak = fagsak, status = BehandlingStatus.FATTER_VEDTAK)
            every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsak.id) } returns åpenBehandling

            // Act & Assert
            assertThrows<RekjørSenereException> {
                autovedtakStegService.kjørAutomatiskBehandlingSøknad(
                    mottakersAktør,
                    søknad,
                    førstegangKjørt = LocalDateTime.now(),
                )
            }
            verify(exactly = 0) { autovedtakSøknadService.kjørBehandling(any()) }
        }

        @Test
        fun `skal opprette oppgave når åpen behandling har status FATTER_VEDTAK og ble endret for 7 dager eller mer siden`() {
            // Arrange
            val åpenBehandling = lagBehandling(fagsak = fagsak, status = BehandlingStatus.FATTER_VEDTAK)
            every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsak.id) } returns åpenBehandling

            // Act
            val resultat =
                autovedtakStegService.kjørAutomatiskBehandlingSøknad(
                    mottakersAktør,
                    søknad,
                    førstegangKjørt = LocalDateTime.now().minusDays(7),
                )

            // Assert
            assertThat(resultat).isEqualTo("Søknad: Bruker har åpen behandling")
            verify(exactly = 0) { autovedtakSøknadService.kjørBehandling(any()) }
            verify(exactly = 1) {
                oppgaveService.opprettOppgaveForManuellBehandling(
                    behandlingId = åpenBehandling.id,
                    begrunnelse = "Søknad: Bruker har åpen behandling",
                    manuellOppgaveType = ManuellOppgaveType.ÅPEN_BEHANDLING,
                    oppgavetype = Oppgavetype.BehandleSak,
                )
            }
        }

        @Test
        fun `skal kaste RekjørSenereException når åpen behandling har status IVERKSETTER_VEDTAK`() {
            // Arrange
            val åpenBehandling = lagBehandling(fagsak = fagsak, status = BehandlingStatus.IVERKSETTER_VEDTAK)
            every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsak.id) } returns åpenBehandling

            // Act & Assert
            assertThrows<RekjørSenereException> {
                autovedtakStegService.kjørAutomatiskBehandlingSøknad(mottakersAktør, søknad)
            }
            verify(exactly = 0) { autovedtakSøknadService.kjørBehandling(any()) }
        }

        @Test
        fun `skal kaste Feil når åpen behandling har en uhåndtert status`() {
            // Arrange
            val åpenBehandling = lagBehandling(fagsak = fagsak, status = BehandlingStatus.AVSLUTTET)
            every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsak.id) } returns åpenBehandling

            // Act & Assert
            assertThrows<Feil> {
                autovedtakStegService.kjørAutomatiskBehandlingSøknad(mottakersAktør, søknad)
            }
            verify(exactly = 0) { autovedtakSøknadService.kjørBehandling(any()) }
        }
    }

    @Nested
    inner class KjørBehandlingFødselshendelse {
        @BeforeEach
        fun setUp() {
            every {
                autovedtakFødselshendelseService.skalAutovedtakBehandles(FødselshendelseData(nyBehandlingHendelse))
            } returns true
            every { fagsakService.hentNormalFagsak(mottakersAktør) } returns fagsak
            every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsak.id) } returns null
            every {
                autovedtakFødselshendelseService.kjørBehandling(FødselshendelseData(nyBehandlingHendelse))
            } returns "Fødselshendelse: Behandling ferdig"
        }

        @Test
        fun `skal returnere tidlig uten å hente fagsak når autovedtak ikke skal behandles`() {
            // Arrange
            every {
                autovedtakFødselshendelseService.skalAutovedtakBehandles(FødselshendelseData(nyBehandlingHendelse))
            } returns false

            // Act
            val resultat = autovedtakStegService.kjørBehandlingFødselshendelse(mottakersAktør, nyBehandlingHendelse)

            // Assert
            assertThat(resultat).isEqualTo("Fødselshendelse: Skal ikke behandles")
            verify(exactly = 0) { fagsakService.hentNormalFagsak(any()) }
            verify(exactly = 0) { autovedtakFødselshendelseService.kjørBehandling(any()) }
        }

        @Test
        fun `skal kjøre behandling og returnere resultat når det ikke finnes noen åpen behandling`() {
            // Act
            val resultat = autovedtakStegService.kjørBehandlingFødselshendelse(mottakersAktør, nyBehandlingHendelse)

            // Assert
            assertThat(resultat).isEqualTo("Fødselshendelse: Behandling ferdig")
            verify(exactly = 1) { autovedtakFødselshendelseService.kjørBehandling(FødselshendelseData(nyBehandlingHendelse)) }
        }

        @Test
        fun `skal opprette oppgave med oppgavetype VurderLivshendelse og returnere at bruker har åpen behandling når man ikke kan snike i køen`() {
            // Arrange
            val åpenBehandling = lagBehandling(fagsak = fagsak, status = BehandlingStatus.UTREDES)
            every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsak.id) } returns åpenBehandling
            every { snikeIKøenService.kanSnikeForbi(åpenBehandling) } returns false

            // Act
            val resultat = autovedtakStegService.kjørBehandlingFødselshendelse(mottakersAktør, nyBehandlingHendelse)

            // Assert
            assertThat(resultat).isEqualTo("Fødselshendelse: Bruker har åpen behandling")
            verify(exactly = 0) { autovedtakFødselshendelseService.kjørBehandling(any()) }
            verify(exactly = 1) {
                oppgaveService.opprettOppgaveForManuellBehandling(
                    behandlingId = åpenBehandling.id,
                    begrunnelse = "Fødselshendelse: Bruker har åpen behandling",
                    manuellOppgaveType = ManuellOppgaveType.ÅPEN_BEHANDLING,
                    oppgavetype = Oppgavetype.VurderLivshendelse,
                )
            }
        }

        @Test
        fun `skal sette åpen behandling på maskinell vent med årsak FØDSELSHENDELSE og likevel kjøre ny behandling når man kan snike i køen`() {
            // Arrange
            val åpenBehandling = lagBehandling(fagsak = fagsak, status = BehandlingStatus.UTREDES)
            every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsak.id) } returns åpenBehandling
            every { snikeIKøenService.kanSnikeForbi(åpenBehandling) } returns true
            every { snikeIKøenService.settAktivBehandlingPåMaskinellVent(any(), any()) } just Runs

            // Act
            val resultat = autovedtakStegService.kjørBehandlingFødselshendelse(mottakersAktør, nyBehandlingHendelse)

            // Assert
            assertThat(resultat).isEqualTo("Fødselshendelse: Behandling ferdig")
            verify(exactly = 1) {
                snikeIKøenService.settAktivBehandlingPåMaskinellVent(åpenBehandling.id, SettPåMaskinellVentÅrsak.FØDSELSHENDELSE)
                autovedtakFødselshendelseService.kjørBehandling(FødselshendelseData(nyBehandlingHendelse))
            }
            verify(exactly = 0) { oppgaveService.opprettOppgaveForManuellBehandling(any(), any(), any(), any(), any()) }
        }
    }
}
