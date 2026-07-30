package no.nav.familie.ba.sak.kjerne.autovedtak

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.datagenerator.defaultFagsak
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.autovedtak.finnmarkstillegg.AutovedtakFinnmarkstilleggService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.AutovedtakFødselshendelseService
import no.nav.familie.ba.sak.kjerne.autovedtak.omregning.AutovedtakBrevService
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendringeøs.AutovedtakSatsendringEøsService
import no.nav.familie.ba.sak.kjerne.autovedtak.småbarnstillegg.AutovedtakSmåbarnstilleggService
import no.nav.familie.ba.sak.kjerne.autovedtak.svalbardtillegg.AutovedtakSvalbardtilleggService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.SettPåMaskinellVentÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.SnikeIKøenService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.prosessering.error.RekjørSenereException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.time.YearMonth

class AutobrevStegServiceTest {
    private val fagsakService = mockk<FagsakService>()
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val oppgaveService = mockk<OppgaveService>()
    private val autovedtakFødselshendelseService = mockk<AutovedtakFødselshendelseService>()
    private val autovedtakBrevService = mockk<AutovedtakBrevService>()
    private val autovedtakSmåbarnstilleggService = mockk<AutovedtakSmåbarnstilleggService>()
    private val autovedtakFinnmarkstilleggService = mockk<AutovedtakFinnmarkstilleggService>()
    private val autovedtakSvalbardtilleggService = mockk<AutovedtakSvalbardtilleggService>()
    private val autovedtakSatsendringEøsService = mockk<AutovedtakSatsendringEøsService>()
    private val snikeIKøenService = mockk<SnikeIKøenService>()
    private val featureToggleService = mockk<FeatureToggleService>()

    val autovedtakStegService =
        AutovedtakStegService(
            fagsakService = fagsakService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            oppgaveService = oppgaveService,
            autovedtakFødselshendelseService = autovedtakFødselshendelseService,
            autovedtakBrevService = autovedtakBrevService,
            autovedtakSmåbarnstilleggService = autovedtakSmåbarnstilleggService,
            snikeIKøenService = snikeIKøenService,
            autovedtakFinnmarkstilleggService = autovedtakFinnmarkstilleggService,
            autovedtakSvalbardtilleggService = autovedtakSvalbardtilleggService,
            autovedtakSatsendringEøsService = autovedtakSatsendringEøsService,
            featureToggleService = featureToggleService,
        )

    @Nested
    inner class KjørBehandlingSmåbarnstillegg {
        @Test
        fun `Skal stoppe autovedtak og opprette oppgave ved åpen behandling som utredes og ikke kan snikes forbi`() {
            val aktør = randomAktør()
            val fagsak = defaultFagsak(aktør)
            val behandling =
                lagBehandling(fagsak = fagsak).also {
                    it.status = BehandlingStatus.UTREDES
                }

            every { autovedtakSmåbarnstilleggService.skalAutovedtakBehandles(SmåbarnstilleggData(aktør)) } returns true
            every { fagsakService.hentNormalFagsak(aktør) } returns fagsak
            every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsakId = fagsak.id) } returns behandling
            every { oppgaveService.opprettOppgaveForManuellBehandling(any(), any(), any(), any()) } returns ""
            every { snikeIKøenService.kanSnikeForbi(any()) } returns false

            autovedtakStegService.kjørBehandlingSmåbarnstillegg(
                mottakersAktør = aktør,
                aktør = aktør,
            )

            verify(exactly = 1) { oppgaveService.opprettOppgaveForManuellBehandling(any(), any(), any(), any()) }
        }

        @Test
        fun `Skal stoppe autovedtak og opprette oppgave etter 7 dager ved åpen behandling med status Fatter vedtak`() {
            val aktør = randomAktør()
            val fagsak = defaultFagsak(aktør)
            val behandling =
                lagBehandling(fagsak = fagsak).also {
                    it.status = BehandlingStatus.FATTER_VEDTAK
                }

            every { autovedtakSmåbarnstilleggService.skalAutovedtakBehandles(SmåbarnstilleggData(aktør)) } returns true
            every { fagsakService.hentNormalFagsak(aktør) } returns fagsak
            every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsakId = fagsak.id) } returns behandling
            every { oppgaveService.opprettOppgaveForManuellBehandling(any(), any(), any(), any()) } returns ""

            assertThrows<RekjørSenereException> {
                autovedtakStegService.kjørBehandlingSmåbarnstillegg(
                    mottakersAktør = aktør,
                    aktør = aktør,
                    førstegangKjørt = LocalDateTime.now().minusDays(6),
                )
            }

            autovedtakStegService.kjørBehandlingSmåbarnstillegg(
                mottakersAktør = aktør,
                aktør = aktør,
                førstegangKjørt = LocalDateTime.now().minusDays(7),
            )

            verify(exactly = 1) { oppgaveService.opprettOppgaveForManuellBehandling(any(), any(), any(), any()) }
        }

        @Test
        fun `Skal stoppe autovedtak ved å kaste feil ved åpen behandling som iverksettes`() {
            val aktør = randomAktør()
            val fagsak = defaultFagsak(aktør)
            val behandling =
                lagBehandling(fagsak = fagsak).also {
                    it.status = BehandlingStatus.IVERKSETTER_VEDTAK
                }

            every { autovedtakSmåbarnstilleggService.skalAutovedtakBehandles(SmåbarnstilleggData(aktør)) } returns true
            every { fagsakService.hentNormalFagsak(aktør) } returns fagsak
            every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsakId = fagsak.id) } returns behandling
            every { oppgaveService.opprettOppgaveForManuellBehandling(any(), any(), any(), any()) } returns ""

            assertThrows<RekjørSenereException> {
                autovedtakStegService.kjørBehandlingSmåbarnstillegg(
                    mottakersAktør = aktør,
                    aktør = aktør,
                )
            }
        }
    }

    @Nested
    inner class KjørBehandlingSatsendringEøs {
        @Test
        fun `skal kaste feil når toggle KAN_KJØRE_SATSENDRING_EØS er skrudd av`() {
            // Arrange
            val aktør = randomAktør()
            every { featureToggleService.isEnabled(FeatureToggle.KAN_KJØRE_SATSENDRING_EØS) } returns false

            // Act & Assert
            assertThatThrownBy {
                autovedtakStegService.kjørBehandlingSatsendringEøs(
                    mottakersAktør = aktør,
                    fagsakId = 1L,
                    utbetalingsland = "PL",
                    satsTidspunkt = YearMonth.of(2026, 1),
                )
            }.isInstanceOf(Feil::class.java)

            verify(exactly = 0) { autovedtakSatsendringEøsService.skalAutovedtakBehandles(any()) }
        }

        @Test
        fun `skal rute til AutovedtakSatsendringEøsService når toggle er på`() {
            // Arrange
            val aktør = randomAktør()
            val fagsak = defaultFagsak(aktør)
            val satsTidspunkt = YearMonth.of(2026, 1)
            val data = SatsendringEøsData(fagsakId = fagsak.id, utbetalingsland = "PL", satsTidspunkt = satsTidspunkt)

            every { featureToggleService.isEnabled(FeatureToggle.KAN_KJØRE_SATSENDRING_EØS) } returns true
            every { autovedtakSatsendringEøsService.skalAutovedtakBehandles(data) } returns true
            every { fagsakService.hentPåFagsakId(fagsak.id) } returns fagsak
            every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsakId = fagsak.id) } returns null
            every { autovedtakSatsendringEøsService.kjørBehandling(data) } returns "Satsendring EØS kjørt OK"

            // Act
            val resultat =
                autovedtakStegService.kjørBehandlingSatsendringEøs(
                    mottakersAktør = aktør,
                    fagsakId = fagsak.id,
                    utbetalingsland = "PL",
                    satsTidspunkt = satsTidspunkt,
                )

            // Assert
            assertThat(resultat).isEqualTo("Satsendring EØS kjørt OK")
            verify(exactly = 1) { autovedtakSatsendringEøsService.kjørBehandling(data) }
        }

        @Test
        fun `skal kaste RekjørSenereException ved åpen behandling som ikke kan snikes forbi`() {
            // Arrange
            val aktør = randomAktør()
            val fagsak = defaultFagsak(aktør)
            val satsTidspunkt = YearMonth.of(2026, 1)
            val data = SatsendringEøsData(fagsakId = fagsak.id, utbetalingsland = "PL", satsTidspunkt = satsTidspunkt)
            val behandling =
                lagBehandling(fagsak = fagsak).also {
                    it.status = BehandlingStatus.UTREDES
                }

            every { featureToggleService.isEnabled(FeatureToggle.KAN_KJØRE_SATSENDRING_EØS) } returns true
            every { autovedtakSatsendringEøsService.skalAutovedtakBehandles(data) } returns true
            every { fagsakService.hentPåFagsakId(fagsak.id) } returns fagsak
            every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsakId = fagsak.id) } returns behandling
            every { snikeIKøenService.kanSnikeForbi(any()) } returns false

            // Act & Assert
            assertThatThrownBy {
                autovedtakStegService.kjørBehandlingSatsendringEøs(
                    mottakersAktør = aktør,
                    fagsakId = fagsak.id,
                    utbetalingsland = "PL",
                    satsTidspunkt = satsTidspunkt,
                )
            }.isInstanceOf(RekjørSenereException::class.java)

            verify(exactly = 0) { autovedtakSatsendringEøsService.kjørBehandling(any()) }
        }

        @Test
        fun `skal sette behandling på maskinell vent med årsak SATSENDRING_EØS når den kan snikes forbi`() {
            // Arrange
            val aktør = randomAktør()
            val fagsak = defaultFagsak(aktør)
            val satsTidspunkt = YearMonth.of(2026, 1)
            val data = SatsendringEøsData(fagsakId = fagsak.id, utbetalingsland = "PL", satsTidspunkt = satsTidspunkt)
            val behandling =
                lagBehandling(fagsak = fagsak).also {
                    it.status = BehandlingStatus.UTREDES
                }

            every { featureToggleService.isEnabled(FeatureToggle.KAN_KJØRE_SATSENDRING_EØS) } returns true
            every { autovedtakSatsendringEøsService.skalAutovedtakBehandles(data) } returns true
            every { fagsakService.hentPåFagsakId(fagsak.id) } returns fagsak
            every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsakId = fagsak.id) } returns behandling
            every { snikeIKøenService.kanSnikeForbi(any()) } returns true
            every { snikeIKøenService.settAktivBehandlingPåMaskinellVent(any(), any()) } returns mockk()
            every { autovedtakSatsendringEøsService.kjørBehandling(data) } returns "Satsendring EØS kjørt OK"

            // Act
            autovedtakStegService.kjørBehandlingSatsendringEøs(
                mottakersAktør = aktør,
                fagsakId = fagsak.id,
                utbetalingsland = "PL",
                satsTidspunkt = satsTidspunkt,
            )

            // Assert
            verify(exactly = 1) {
                snikeIKøenService.settAktivBehandlingPåMaskinellVent(behandling.id, SettPåMaskinellVentÅrsak.SATSENDRING_EØS)
            }
        }
    }
}
