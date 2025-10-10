package no.nav.familie.ba.sak.kjerne.autovedtak

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.fake.FakePersonopplysningerService.Companion.leggTilPersonInfo
import no.nav.familie.ba.sak.fake.FakeTaskRepositoryWrapper
import no.nav.familie.ba.sak.fake.tilKonkretTask
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
import no.nav.familie.ba.sak.kjørbehandling.kjørStegprosessForFGB
import no.nav.familie.ba.sak.kjørbehandling.kjørStegprosessForRevurderingÅrligKontroll
import no.nav.familie.ba.sak.task.DistribuerDokumentDTO
import no.nav.familie.ba.sak.task.DistribuerDokumentTask
import no.nav.familie.ba.sak.task.FerdigstillBehandlingTask
import no.nav.familie.ba.sak.task.JournalførVedtaksbrevTask
import no.nav.familie.ba.sak.task.JournalførVedtaksbrevTask.Companion.opprettTaskJournalførVedtaksbrev
import no.nav.familie.ba.sak.task.OpprettOppgaveTask
import no.nav.familie.ba.sak.task.dto.ManuellOppgaveType
import no.nav.familie.ba.sak.task.dto.OpprettOppgaveTaskDTO
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.error.RekjørSenereException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Properties

@ActiveProfiles("snike-i-koen-test-config")
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
    private val brevmalService: BrevmalService,
    @Autowired
    private val vedtaksperiodeService: VedtaksperiodeService,
    @Autowired
    private val behandlingRepository: BehandlingRepository,
    @Autowired
    private val fakeTaskRepositoryWrapper: FakeTaskRepositoryWrapper,
    @Autowired
    private val journalførVedtaksbrevTask: JournalførVedtaksbrevTask,
    @Autowired
    private val distribuerDokumentTask: DistribuerDokumentTask,
    @Autowired
    private val ferdigstillBehandlingTask: FerdigstillBehandlingTask,
    @Autowired
    private val loggRepository: LoggRepository,
) : AbstractSpringIntegrationTest() {
    @Test
    fun `automatisk behandling sniker foran åpen behandling før besluttersteget og setter den tilbake til vilkårsvurderingssteget`() {
        val åpenBehandling = fullførFørstegangsbehandlingOgKjørRevurderingTilSteg(StegType.BEHANDLINGSRESULTAT)

        SnikeIKøenServiceTestConfig.endringstidspunktMock = LocalDateTime.now().minusHours(4)

        assertEquals(
            BEHANDLING_FERDIG,
            autovedtakStegService.kjørBehandlingOmregning(
                åpenBehandling.fagsak.aktør,
                OmregningBrevData(
                    aktør = åpenBehandling.fagsak.aktør,
                    behandlingsårsak = BehandlingÅrsak.OMREGNING_18ÅR,
                    standardbegrunnelse = Standardbegrunnelse.REDUKSJON_UNDER_18_ÅR_AUTOVEDTAK,
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

        val omregningBehandling = behandlingRepository.finnBehandlinger(åpenBehandling.fagsak.id).single { it.opprettetÅrsak == BehandlingÅrsak.OMREGNING_18ÅR }
        fullførTasksForBehandling(omregningBehandling)

        val åpenBehandlingEtterAutomatiskOmregning = behandlingRepository.finnBehandling(åpenBehandling.id)

        assertEquals(StegType.VILKÅRSVURDERING, åpenBehandlingEtterAutomatiskOmregning.steg)
        assertEquals(
            LoggType.BEHANDLING_TATT_AV_MASKINELL_VENT,
            loggRepository.hentLoggForBehandling(åpenBehandling.id).maxBy { it.id }.type,
        )
    }

    @Test
    fun `automatisk behandling forsøker å rekjøre og avbrytes med manuell oppgave etter 7 dager med åpen behandling på besluttersteget`() {
        val åpenBehandling = fullførFørstegangsbehandlingOgKjørRevurderingTilSteg(StegType.SEND_TIL_BESLUTTER)

        val tid6DagerSiden = LocalDateTime.now().minusDays(6)

        assertThrows<RekjørSenereException> {
            autovedtakStegService.kjørBehandlingOmregning(
                åpenBehandling.fagsak.aktør,
                OmregningBrevData(
                    aktør = åpenBehandling.fagsak.aktør,
                    behandlingsårsak = BehandlingÅrsak.OMREGNING_18ÅR,
                    standardbegrunnelse = Standardbegrunnelse.REDUKSJON_UNDER_18_ÅR_AUTOVEDTAK,
                    fagsakId = åpenBehandling.fagsak.id,
                ),
                førstegangKjørt = tid6DagerSiden,
            )
        }

        autovedtakStegService.kjørBehandlingOmregning(
            åpenBehandling.fagsak.aktør,
            OmregningBrevData(
                aktør = åpenBehandling.fagsak.aktør,
                behandlingsårsak = BehandlingÅrsak.OMREGNING_18ÅR,
                standardbegrunnelse = Standardbegrunnelse.REDUKSJON_UNDER_18_ÅR_AUTOVEDTAK,
                fagsakId = åpenBehandling.fagsak.id,
            ),
            førstegangKjørt = tid6DagerSiden.minusDays(1),
        )

        val lagredeTaskerAvType =
            fakeTaskRepositoryWrapper
                .hentLagredeTaskerAvType(OpprettOppgaveTask.TASK_STEP_TYPE)
                .tilKonkretTask<OpprettOppgaveTaskDTO>()

        val lagretTask = lagredeTaskerAvType.singleOrNull { it.behandlingId == åpenBehandling.id && it.oppgavetype == Oppgavetype.VurderLivshendelse && it.manuellOppgaveType == ManuellOppgaveType.ÅPEN_BEHANDLING }

        assertThat(lagretTask).isNotNull()
    }

    private fun fullførFørstegangsbehandlingOgKjørRevurderingTilSteg(stegType: StegType): Behandling {
        val søkerFnr = leggTilPersonInfo(fødselsdato = LocalDate.now().minusYears(30))
        val barn18år = leggTilPersonInfo(fødselsdato = LocalDate.now().minusYears(18).withDayOfMonth(10))
        val barn2år = leggTilPersonInfo(fødselsdato = LocalDate.now().minusYears(6).withDayOfMonth(10))
        val barnasIdenter = listOf(barn18år, barn2år)

        val fagsak = kjørFørstegangsbehandling(søkerFnr = søkerFnr, barnasIdenter = barnasIdenter).fagsak
        return kjørRevurderingTilSteg(stegType, fagsak.id, søkerFnr, barnasIdenter)
    }

    private fun fullførTasksForBehandling(behandling: Behandling) {
        val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandling.id)
        journalførVedtaksbrevTask.doTask(opprettTaskJournalførVedtaksbrev(personIdent = behandling.fagsak.aktør.aktivFødselsnummer(), behandling.id, vedtakId = vedtak!!.id))
        distribuerDokumentTask.doTask(
            DistribuerDokumentTask.opprettDistribuerDokumentTask(
                distribuerDokumentDTO =
                    DistribuerDokumentDTO(
                        fagsakId = behandling.fagsak.id,
                        behandlingId = behandling.id,
                        journalpostId = "",
                        brevmal = brevmalService.hentBrevmal(behandling),
                        erManueltSendt = false,
                        manuellAdresseInfo = null,
                    ),
                properties = Properties(),
            ),
        )
        ferdigstillBehandlingTask.doTask(
            FerdigstillBehandlingTask.opprettTask(
                søkerIdent = behandling.fagsak.aktør.aktivFødselsnummer(),
                behandlingsId = behandling.id,
            ),
        )
    }

    private fun kjørFørstegangsbehandling(
        søkerFnr: String,
        barnasIdenter: List<String>,
    ): Behandling =
        kjørStegprosessForFGB(
            tilSteg = StegType.BEHANDLING_AVSLUTTET,
            søkerFnr = søkerFnr,
            barnasIdenter = barnasIdenter,
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            persongrunnlagService = persongrunnlagService,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
            brevmalService = brevmalService,
            vilkårInnvilgetFom = LocalDate.now().withDayOfMonth(10).minusYears(6),
        )

    private fun kjørRevurderingTilSteg(
        steg: StegType,
        fagsakId: Long,
        søkerFnr: String,
        barnasIdenter: List<String>,
    ): Behandling =
        kjørStegprosessForRevurderingÅrligKontroll(
            tilSteg = steg,
            søkerFnr = søkerFnr,
            barnasIdenter = barnasIdenter,
            vedtakService = vedtakService,
            stegService = stegService,
            fagsakId = fagsakId,
            brevmalService = brevmalService,
            vedtaksperiodeService = vedtaksperiodeService,
        )
}
