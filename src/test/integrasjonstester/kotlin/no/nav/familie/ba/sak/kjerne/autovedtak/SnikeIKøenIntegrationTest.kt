package no.nav.familie.ba.sak.kjerne.autovedtak

import io.mockk.verify
import no.nav.familie.ba.sak.common.guttenBarnesenFødselsdato
import no.nav.familie.ba.sak.common.kjørStegprosessForFGB
import no.nav.familie.ba.sak.common.kjørStegprosessForRevurderingÅrligKontroll
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakStegService.Companion.BEHANDLING_FERDIG
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.logg.LoggRepository
import no.nav.familie.ba.sak.kjerne.logg.LoggType
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.task.DistribuerDokumentTask
import no.nav.familie.ba.sak.task.FerdigstillBehandlingTask
import no.nav.familie.ba.sak.task.JournalførVedtaksbrevTask
import no.nav.familie.prosessering.domene.Task
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class SnikeIKøenIntegrationTest(
    @Autowired
    private val stegService: StegService,
    @Autowired
    private val vedtakService: VedtakService,
    @Autowired
    private val autovedtakStegService: AutovedtakStegService,
    @Autowired
    private val persongrunnlagService: PersongrunnlagService,
    @Autowired
    private val fagsakService: FagsakService,
    @Autowired
    private val vilkårsvurderingService: VilkårsvurderingService,
    @Autowired
    private val databaseCleanupService: DatabaseCleanupService,
    @Autowired
    private val brevmalService: BrevmalService,
    @Autowired
    private val vedtaksperiodeService: VedtaksperiodeService,
    @Autowired
    private val behandlingRepository: BehandlingRepository,
    @Autowired
    private val taskRepository: TaskRepositoryWrapper,
    @Autowired
    private val journalførVedtaksbrevTask: JournalførVedtaksbrevTask,
    @Autowired
    private val distribuerDokumentTask: DistribuerDokumentTask,
    @Autowired
    private val ferdigstillBehandlingTask: FerdigstillBehandlingTask,
    @Autowired
    private val loggRepository: LoggRepository,
) : AbstractSpringIntegrationTest() {
    val søkerFnr = randomFnr()
    val barnFnr = ClientMocks.barnFnr[0]

    @BeforeEach
    fun init() {
        databaseCleanupService.truncate()
    }

    @Test
    fun `automatisk behandling for omregning skal snike foran og sette åpen behandling før besluttersteget tilbake til vilkårsvurderingssteget`() {
        val åpenBehandling = kjørFørstegangsbehandlingOgSåRevurderingTilStegBehandlingsresultat()

        assertEquals(
            BEHANDLING_FERDIG,
            autovedtakStegService.kjørBehandlingOmregning(
                åpenBehandling.fagsak.aktør,
                OmregningBrevData(
                    aktør = åpenBehandling.fagsak.aktør,
                    behandlingsårsak = BehandlingÅrsak.OMREGNING_6ÅR,
                    standardbegrunnelse = Standardbegrunnelse.REDUKSJON_UNDER_6_ÅR_AUTOVEDTAK,
                    fagsakId = åpenBehandling.fagsak.id,
                ),
            ),
        )
        assertEquals(
            BehandlingStatus.SATT_PÅ_MASKINELL_VENT,
            behandlingRepository.finnBehandling(åpenBehandling.id).status,
        )
        assertEquals(
            LoggType.BEHANDLING_SATT_PÅ_MASKINELL_VENT,
            loggRepository.hentLoggForBehandling(åpenBehandling.id).maxBy { it.id }.type,
        )

        fullførTasks()

        val åpenBehandlingEtterAutomatiskOmregning = behandlingRepository.finnBehandling(åpenBehandling.id)

        assertEquals(StegType.VILKÅRSVURDERING, åpenBehandlingEtterAutomatiskOmregning.steg)
        assertEquals(
            LoggType.BEHANDLING_TATT_AV_MASKINELL_VENT,
            loggRepository.hentLoggForBehandling(åpenBehandling.id).maxBy { it.id }.type,
        )
    }

    private fun fullførTasks() {
        val opprettedeTasks = mutableListOf<Task>()

        for (i in 0..2) {
            verify {
                taskRepository.save(capture(opprettedeTasks))
            }
            val nesteTask = opprettedeTasks.last()
            when (nesteTask.type) {
                JournalførVedtaksbrevTask.TASK_STEP_TYPE -> {
                    journalførVedtaksbrevTask.doTask(nesteTask)
                }

                DistribuerDokumentTask.TASK_STEP_TYPE -> {
                    distribuerDokumentTask.doTask(nesteTask)
                }

                FerdigstillBehandlingTask.TASK_STEP_TYPE -> {
                    ferdigstillBehandlingTask.doTask(nesteTask)
                }
            }
        }
    }

    private fun kjørFørstegangsbehandlingOgSåRevurderingTilStegBehandlingsresultat(): Behandling {
        val førstegangsbehandling =
            kjørStegprosessForFGB(
                tilSteg = StegType.BEHANDLING_AVSLUTTET,
                søkerFnr = søkerFnr,
                barnasIdenter = listOf(barnFnr),
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService,
                vedtaksperiodeService = vedtaksperiodeService,
                brevmalService = brevmalService,
                vilkårInnvilgetFom = guttenBarnesenFødselsdato,
            )

        return kjørStegprosessForRevurderingÅrligKontroll(
            tilSteg = StegType.BEHANDLINGSRESULTAT,
            søkerFnr = søkerFnr,
            barnasIdenter = listOf(barnFnr),
            vedtakService = vedtakService,
            stegService = stegService,
            fagsakId = førstegangsbehandling.fagsak.id,
            brevmalService = brevmalService,
        )
    }
}
