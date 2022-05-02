package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.config.IntegrasjonClientMock
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.FiltreringsreglerService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.StatsborgerskapService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FødselshendelseServiceTest {
    val filtreringsreglerService = mockk<FiltreringsreglerService>()
    val taskRepository = mockk<TaskRepositoryWrapper>()
    val behandlingRepository = mockk<BehandlingRepository>()
    val fagsakService = mockk<FagsakService>()
    val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    val vilkårsvurderingRepository = mockk<VilkårsvurderingRepository>()
    val persongrunnlagService = mockk<PersongrunnlagService>()
    val personidentService = mockk<PersonidentService>()
    val stegService = mockk<StegService>()
    val vedtakService = mockk<VedtakService>()
    val vedtaksperiodeService = mockk<VedtaksperiodeService>()
    val autovedtakService = mockk<AutovedtakService>()
    val personopplysningerService = mockk<PersonopplysningerService>()
    val opprettTaskService = mockk<OpprettTaskService>()
    val oppgaveService = mockk<OppgaveService>()

    val integrasjonClient = mockk<IntegrasjonClient>()
    val statsborgerskapService = StatsborgerskapService(
        integrasjonClient = integrasjonClient
    )

    private val autovedtakFødselshendelseService = AutovedtakFødselshendelseService(
        fagsakService,
        behandlingHentOgPersisterService,
        filtreringsreglerService,
        taskRepository,
        vilkårsvurderingRepository,
        persongrunnlagService,
        personidentService,
        stegService,
        vedtakService,
        vedtaksperiodeService,
        autovedtakService,
        personopplysningerService,
        statsborgerskapService,
        opprettTaskService,
        oppgaveService
    )

    @Test
    fun `Skal opprette fremleggsoppgave dersom søker er EØS medlem`() {
        every { personopplysningerService.hentGjeldendeStatsborgerskap(any()) } returns Statsborgerskap(
            land = "POL",
            gyldigFraOgMed = LocalDate.now().minusMonths(2),
            gyldigTilOgMed = null,
            bekreftelsesdato = null
        )
        every { integrasjonClient.hentAlleEØSLand() } returns IntegrasjonClientMock.hentKodeverkLand()
        every { opprettTaskService.opprettOppgaveTask(any(), any(), any(), any()) } just runs

        autovedtakFødselshendelseService.opprettFremleggsoppgaveDersomEØSMedlem(lagBehandling())

        verify(exactly = 1) {
            opprettTaskService.opprettOppgaveTask(
                behandlingId = any(),
                oppgavetype = Oppgavetype.Fremlegg,
                beskrivelse = "Kontroller gyldig opphold",
                fristForFerdigstillelse = LocalDate.now().plusYears(1)
            )
        }
    }

    @Test
    fun `Skal ikke opprette fremleggsoppgave dersom søker er nordisk medlem`() {
        every { personopplysningerService.hentGjeldendeStatsborgerskap(any()) } returns Statsborgerskap(
            land = "DNK",
            gyldigFraOgMed = LocalDate.now().minusMonths(2),
            gyldigTilOgMed = null,
            bekreftelsesdato = null
        )
        every { integrasjonClient.hentAlleEØSLand() } returns IntegrasjonClientMock.hentKodeverkLand()
        every { opprettTaskService.opprettOppgaveTask(any(), any(), any(), any()) } just runs

        autovedtakFødselshendelseService.opprettFremleggsoppgaveDersomEØSMedlem(lagBehandling())

        verify(exactly = 0) {
            opprettTaskService.opprettOppgaveTask(
                behandlingId = any(),
                oppgavetype = Oppgavetype.Fremlegg,
                beskrivelse = "Kontroller gyldig opphold",
                fristForFerdigstillelse = LocalDate.now().plusYears(1)
            )
        }
    }
}
