package no.nav.familie.ba.sak.kjerne.autovedtak

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.LocalDateProvider
import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.datagenerator.settpåvent.lagSettPåVent
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.integrasjoner.oppgave.domene.OppgaveRepository
import no.nav.familie.ba.sak.kjerne.autovedtak.småbarnstillegg.AutovedtakSmåbarnstilleggService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentService
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.SmåbarnstilleggService
import no.nav.familie.ba.sak.kjerne.beregning.erEndringIOvergangsstønadFramITid
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AutovedtakSmåbarnstilleggServiceTest {
    private val fagsakService: FagsakService = mockk<FagsakService>()
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val vedtakService: VedtakService = mockk<VedtakService>()
    private val behandlingService: BehandlingService = mockk<BehandlingService>()
    private val vedtaksperiodeService: VedtaksperiodeService = mockk<VedtaksperiodeService>()
    private val småbarnstilleggService: SmåbarnstilleggService = mockk<SmåbarnstilleggService>()
    private val taskService: TaskService = mockk<TaskService>()
    private val beregningService: BeregningService = mockk<BeregningService>()
    private val autovedtakService: AutovedtakService = mockk<AutovedtakService>()
    private val vedtaksperiodeHentOgPersisterService: VedtaksperiodeHentOgPersisterService = mockk<VedtaksperiodeHentOgPersisterService>()
    private val localDateProvider: LocalDateProvider = mockk<LocalDateProvider>()
    private val påVentService: SettPåVentService = mockk<SettPåVentService>()
    private val opprettTaskService: OpprettTaskService = mockk<OpprettTaskService>()
    private val loggService: LoggService = mockk<LoggService>()

    val oppgaveService =
        OppgaveService(
            oppgaveRepository = mockk<OppgaveRepository>(),
            behandlingRepository = mockk<BehandlingRepository>(),
            integrasjonClient = mockk<IntegrasjonClient>(),
            arbeidsfordelingPåBehandlingRepository = mockk(),
            opprettTaskService = opprettTaskService,
            loggService = loggService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
        )

    val autovedtakSmåbarnstilleggService =
        AutovedtakSmåbarnstilleggService(
            fagsakService = fagsakService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            vedtakService = vedtakService,
            behandlingService = behandlingService,
            vedtaksperiodeService = vedtaksperiodeService,
            småbarnstilleggService = småbarnstilleggService,
            taskService = taskService,
            beregningService = beregningService,
            autovedtakService = autovedtakService,
            oppgaveService = oppgaveService,
            vedtaksperiodeHentOgPersisterService = vedtaksperiodeHentOgPersisterService,
            localDateProvider = localDateProvider,
            påVentService = påVentService,
            opprettTaskService = opprettTaskService,
            loggService = loggService,
        )

    @Test
    fun `skal sette forrige behandling tilbake til påVent, henlegge behandling og opprette småbarnstilleggOppgave dersom behandling ikke greier å snike forbi behandling som er på vent`() {
        val aktør = randomAktør()
        val fagsak = defaultFagsak(aktør = aktør)
        val forrigeBehandling = lagBehandling(fagsak = fagsak, status = BehandlingStatus.SATT_PÅ_MASKINELL_VENT)
        val behandling = lagBehandling(fagsak = fagsak, status = BehandlingStatus.UTREDES)
        val behandlingsData = SmåbarnstilleggData(aktør = aktør)
        val settPåVent = lagSettPåVent(forrigeBehandling)

        every { fagsakService.hentNormalFagsak(aktør) } returns fagsak
        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns forrigeBehandling
        every { småbarnstilleggService.hentPerioderMedFullOvergangsstønad(forrigeBehandling) } returns emptyList()
        every { småbarnstilleggService.hentPerioderMedFullOvergangsstønad(aktør) } returns emptyList()
        every { localDateProvider.now() } returns LocalDate.now()
        mockkStatic(::erEndringIOvergangsstønadFramITid)
        every { erEndringIOvergangsstønadFramITid(any(), any(), any()) } returns false
        every { autovedtakService.opprettAutomatiskBehandlingOgKjørTilBehandlingsresultat(any(), any(), any(), any()) } returns behandling
        every { behandlingHentOgPersisterService.hentBehandlinger(fagsak.id, BehandlingStatus.SATT_PÅ_MASKINELL_VENT) } returns listOf(forrigeBehandling)
        every { påVentService.finnAktivSettPåVentPåBehandling(forrigeBehandling.id) } returns settPåVent

        val behandlingSlot = slot<Behandling>()
        every { behandlingHentOgPersisterService.lagreEllerOppdater(capture(behandlingSlot)) } returns forrigeBehandling.copy(status = BehandlingStatus.SATT_PÅ_VENT)

        every { opprettTaskService.opprettHenleggBehandlingTask(any(), any(), any()) } just Runs
        every { opprettTaskService.opprettOppgaveForManuellBehandlingTask(any(), any(), any(), any()) } just Runs
        every { loggService.opprettAutovedtakTilManuellBehandling(any(), any()) } just Runs

        autovedtakSmåbarnstilleggService.kjørBehandling(behandlingsdata = behandlingsData)

        assertThat(behandlingSlot.captured.status).isEqualTo(BehandlingStatus.SATT_PÅ_VENT)
        verify(exactly = 1) { opprettTaskService.opprettHenleggBehandlingTask(any(), any(), any()) }
        verify(exactly = 1) { opprettTaskService.opprettOppgaveForManuellBehandlingTask(any(), any(), any(), any()) }
    }
}
