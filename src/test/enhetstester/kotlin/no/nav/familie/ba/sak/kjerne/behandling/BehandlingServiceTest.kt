package no.nav.familie.ba.sak.kjerne.behandling

import io.mockk.called
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.config.FeatureToggle
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.lagNyBehandling
import no.nav.familie.ba.sak.datagenerator.lagNyEksternBehandlingRelasjon
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.behandling.behandlingstema.BehandlingstemaService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingMigreringsinfoRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingSøknadsinfoService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.domene.EksternBehandlingRelasjon
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.statistikk.saksstatistikk.SaksstatistikkEventPublisher
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.YearMonth
import java.util.UUID

class BehandlingServiceTest {
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService = mockk()
    private val behandlingstemaService: BehandlingstemaService = mockk()
    private val behandlingSøknadsinfoService: BehandlingSøknadsinfoService = mockk()
    private val behandlingMigreringsinfoRepository: BehandlingMigreringsinfoRepository = mockk()
    private val behandlingMetrikker: BehandlingMetrikker = mockk()
    private val saksstatistikkEventPublisher: SaksstatistikkEventPublisher = mockk()
    private val fagsakRepository: FagsakRepository = mockk()
    private val vedtakRepository: VedtakRepository = mockk()
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository = mockk()
    private val loggService: LoggService = mockk()
    private val arbeidsfordelingService: ArbeidsfordelingService = mockk()
    private val infotrygdService: InfotrygdService = mockk()
    private val vedtaksperiodeService: VedtaksperiodeService = mockk()
    private val taskRepository: TaskRepositoryWrapper = mockk()
    private val vilkårsvurderingService: VilkårsvurderingService = mockk()
    private val featureToggleService: FeatureToggleService = mockk()
    private val eksternBehandlingRelasjonService = mockk<EksternBehandlingRelasjonService>()

    private val behandlingService: BehandlingService =
        BehandlingService(
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            behandlingstemaService = behandlingstemaService,
            behandlingSøknadsinfoService = behandlingSøknadsinfoService,
            behandlingMigreringsinfoRepository = behandlingMigreringsinfoRepository,
            behandlingMetrikker = behandlingMetrikker,
            saksstatistikkEventPublisher = saksstatistikkEventPublisher,
            fagsakRepository = fagsakRepository,
            vedtakRepository = vedtakRepository,
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
            loggService = loggService,
            arbeidsfordelingService = arbeidsfordelingService,
            infotrygdService = infotrygdService,
            vedtaksperiodeService = vedtaksperiodeService,
            taskRepository = taskRepository,
            vilkårsvurderingService = vilkårsvurderingService,
            featureToggleService = featureToggleService,
            eksternBehandlingRelasjonService = eksternBehandlingRelasjonService,
        )

    @Nested
    inner class OpprettBehandling {
        @Test
        fun `skal opprette revurdering og lagre ekstern behandling relasjon`() {
            // Arrange
            val fagsak = lagFagsak()

            val førstegangsbehandling =
                lagBehandling(
                    fagsak = fagsak,
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    årsak = BehandlingÅrsak.SØKNAD,
                )

            val nyBehandling =
                lagNyBehandling(
                    fagsakId = fagsak.id,
                    behandlingType = BehandlingType.REVURDERING,
                    behandlingÅrsak = BehandlingÅrsak.KLAGE,
                    nyEksternBehandlingRelasjon =
                        lagNyEksternBehandlingRelasjon(
                            eksternBehandlingId = UUID.randomUUID().toString(),
                            eksternBehandlingFagsystem = EksternBehandlingRelasjon.Fagsystem.KLAGE,
                        ),
                )

            val eksternBehandlingRelasjonSlot = slot<EksternBehandlingRelasjon>()

            every { fagsakRepository.finnFagsak(nyBehandling.fagsakId) } returns fagsak
            every { behandlingHentOgPersisterService.finnAktivForFagsak(nyBehandling.fagsakId) } returns null
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(nyBehandling.fagsakId) } returns førstegangsbehandling
            every { behandlingstemaService.finnBehandlingKategori(nyBehandling.fagsakId) } returns BehandlingKategori.NASJONAL
            every { behandlingstemaService.finnLøpendeUnderkategoriFraForrigeVedtatteBehandling(nyBehandling.fagsakId) } returns BehandlingUnderkategori.ORDINÆR
            every { behandlingstemaService.finnUnderkategoriFraAktivBehandling(nyBehandling.fagsakId) } returns BehandlingUnderkategori.ORDINÆR
            every { behandlingHentOgPersisterService.lagreEllerOppdater(any(), any()) } returnsArgument 0
            every { arbeidsfordelingService.fastsettBehandlendeEnhet(any(), any()) } just runs
            every { behandlingMetrikker.tellNøkkelTallVedOpprettelseAvBehandling(any()) } just runs
            every { eksternBehandlingRelasjonService.lagreEksternBehandlingRelasjon(capture(eksternBehandlingRelasjonSlot)) } returnsArgument 0
            every { behandlingSøknadsinfoService.lagreSøknadsinfo(any(), any(), any()) } just runs
            every { saksstatistikkEventPublisher.publiserBehandlingsstatistikk(any()) } just runs
            every { vedtakRepository.findByBehandlingAndAktivOptional(any()) } returns null
            every { vedtaksperiodeService.kopierOverVedtaksperioder(any(), any()) } just runs
            every { vedtakRepository.save(any()) } returnsArgument 0
            every { loggService.opprettBehandlingLogg(any()) } just runs
            every { taskRepository.save(any()) } returnsArgument 0
            every { featureToggleService.isEnabled(FeatureToggle.SJEKK_AKTIV_INFOTRYGD_SAK_REPLIKA, true) } returns false
            every { featureToggleService.isEnabled(FeatureToggle.PREUTFYLLING_VILKÅR) } returns true

            // Act
            val opprettetBehandling = behandlingService.opprettBehandling(nyBehandling)

            // Assert
            assertThat(opprettetBehandling.id).isEqualTo(0L)
            val eksternBehandlingRelasjon = eksternBehandlingRelasjonSlot.captured
            assertThat(eksternBehandlingRelasjon.id).isEqualTo(0L)
            assertThat(eksternBehandlingRelasjon.internBehandlingId).isEqualTo(opprettetBehandling.id)
            assertThat(eksternBehandlingRelasjon.eksternBehandlingId).isEqualTo(nyBehandling.nyEksternBehandlingRelasjon?.eksternBehandlingId)
            assertThat(eksternBehandlingRelasjon.eksternBehandlingFagsystem).isEqualTo(nyBehandling.nyEksternBehandlingRelasjon?.eksternBehandlingFagsystem)
            assertThat(eksternBehandlingRelasjon.opprettetTid).isNotNull()
        }

        @Test
        fun `skal opprette revurdering og men ikke lagre ekstern behandling relasjon`() {
            // Arrange
            val fagsak = lagFagsak()

            val førstegangsbehandling =
                lagBehandling(
                    fagsak = fagsak,
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    årsak = BehandlingÅrsak.SØKNAD,
                )

            val nyBehandling =
                lagNyBehandling(
                    fagsakId = fagsak.id,
                    behandlingType = BehandlingType.REVURDERING,
                    behandlingÅrsak = BehandlingÅrsak.KLAGE,
                    nyEksternBehandlingRelasjon = null,
                )

            every { fagsakRepository.finnFagsak(nyBehandling.fagsakId) } returns fagsak
            every { behandlingHentOgPersisterService.finnAktivForFagsak(nyBehandling.fagsakId) } returns null
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(nyBehandling.fagsakId) } returns førstegangsbehandling
            every { behandlingstemaService.finnBehandlingKategori(nyBehandling.fagsakId) } returns BehandlingKategori.NASJONAL
            every { behandlingstemaService.finnLøpendeUnderkategoriFraForrigeVedtatteBehandling(nyBehandling.fagsakId) } returns BehandlingUnderkategori.ORDINÆR
            every { behandlingstemaService.finnUnderkategoriFraAktivBehandling(nyBehandling.fagsakId) } returns BehandlingUnderkategori.ORDINÆR
            every { behandlingHentOgPersisterService.lagreEllerOppdater(any(), any()) } returnsArgument 0
            every { arbeidsfordelingService.fastsettBehandlendeEnhet(any(), any()) } just runs
            every { behandlingMetrikker.tellNøkkelTallVedOpprettelseAvBehandling(any()) } just runs
            every { behandlingSøknadsinfoService.lagreSøknadsinfo(any(), any(), any()) } just runs
            every { saksstatistikkEventPublisher.publiserBehandlingsstatistikk(any()) } just runs
            every { vedtakRepository.findByBehandlingAndAktivOptional(any()) } returns null
            every { vedtaksperiodeService.kopierOverVedtaksperioder(any(), any()) } just runs
            every { vedtakRepository.save(any()) } returnsArgument 0
            every { loggService.opprettBehandlingLogg(any()) } just runs
            every { taskRepository.save(any()) } returnsArgument 0
            every { featureToggleService.isEnabled(FeatureToggle.SJEKK_AKTIV_INFOTRYGD_SAK_REPLIKA, true) } returns false
            every { featureToggleService.isEnabled(FeatureToggle.PREUTFYLLING_VILKÅR) } returns true

            // Act
            val opprettetBehandling = behandlingService.opprettBehandling(nyBehandling)

            // Assert
            assertThat(opprettetBehandling.id).isEqualTo(0L)
            verify { eksternBehandlingRelasjonService wasNot called }
        }
    }

    @Nested
    inner class ErLøpende {
        @Test
        fun `skal returnere true dersom det finnes andeler i en behandling hvor tom er etter YearMonth now`() {
            // Arrange
            val behandling = lagBehandling()

            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(any()) } returns
                listOf(
                    lagAndelTilkjentYtelse(YearMonth.now().minusYears(1), YearMonth.now().minusMonths(6)),
                    lagAndelTilkjentYtelse(YearMonth.now().minusMonths(6), YearMonth.now().minusMonths(3)),
                    lagAndelTilkjentYtelse(YearMonth.now().minusMonths(3), YearMonth.now().plusMonths(3)),
                )

            // Act
            val erLøpende = behandlingService.erLøpende(behandling)

            // Assert
            assertThat(erLøpende).isTrue()
        }

        @Test
        fun `skal returnere false dersom det finnes andeler i en behandling hvor tom er det samme som YearMonth now`() {
            // Arrange
            val behandling = lagBehandling()

            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(any()) } returns
                listOf(
                    lagAndelTilkjentYtelse(YearMonth.now().minusYears(1), YearMonth.now().minusMonths(6)),
                    lagAndelTilkjentYtelse(YearMonth.now().minusMonths(6), YearMonth.now().minusMonths(3)),
                    lagAndelTilkjentYtelse(YearMonth.now().minusMonths(3), YearMonth.now()),
                )

            // Act
            val erLøpende = behandlingService.erLøpende(behandling)

            // Assert
            assertThat(erLøpende).isFalse()
        }

        @Test
        fun `skal returnere false dersom alle andeler i en behandling har tom før YearMonth now`() {
            // Arrange
            val behandling = lagBehandling()

            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(any()) } returns
                listOf(
                    lagAndelTilkjentYtelse(YearMonth.now().minusYears(1), YearMonth.now().minusMonths(6)),
                    lagAndelTilkjentYtelse(YearMonth.now().minusMonths(6), YearMonth.now().minusMonths(3)),
                    lagAndelTilkjentYtelse(YearMonth.now().minusMonths(3), YearMonth.now().minusMonths(1)),
                )

            // Act
            val erLøpende = behandlingService.erLøpende(behandling)

            // Assert
            assertThat(erLøpende).isFalse()
        }
    }
}
