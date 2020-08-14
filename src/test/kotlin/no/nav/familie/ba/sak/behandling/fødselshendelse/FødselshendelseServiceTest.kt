package no.nav.familie.ba.sak.behandling.fødselshendelse

import io.mockk.*
import no.nav.familie.ba.sak.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.infotrygd.InfotrygdBarnetrygdClient
import no.nav.familie.ba.sak.infotrygd.InfotrygdFeedService
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.pdl.internal.IdentInformasjon
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.ba.sak.task.OpprettOppgaveTask
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.nare.core.evaluations.Evaluering
import no.nav.nare.core.evaluations.Resultat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class FødselshendelseServiceTest {
    val infotrygdBarnetrygdClientMock = mockk<InfotrygdBarnetrygdClient>()
    val personopplysningerServiceMock= mockk<PersonopplysningerService>()
    val infotrygdFeedServiceMock = mockk<InfotrygdFeedService>()
    val featureToggleServiceMock = mockk<FeatureToggleService>()
    val stegServiceMock = mockk<StegService>()
    val vedtakServiceMock = mockk<VedtakService>()
    val evaluerFiltreringsreglerForFødselshendelseMock = mockk<EvaluerFiltreringsreglerForFødselshendelse>()
    val taskRepositoryMock = mockk<TaskRepository>()

    val søkerFnr = "12345678910"
    val barn1Fnr = "12345678911"
    val barn2Fnr = "12345678912"

    val fødselshendelseService = FødselshendelseService(infotrygdFeedServiceMock,
                                                        infotrygdBarnetrygdClientMock,
                                                        featureToggleServiceMock,
                                                        stegServiceMock,
                                                        vedtakServiceMock,
                                                        evaluerFiltreringsreglerForFødselshendelseMock,
                                                        taskRepositoryMock,
                                                        personopplysningerServiceMock)

    @Test
    fun `fødselshendelseSkalBehandlesHosInfotrygd skal returne true dersom klienten returnerer false`() {
        every { personopplysningerServiceMock.hentIdenter(any()) } returns listOf(IdentInformasjon(søkerFnr, false, "FOLKEREGISTERIDENT"))
        every { infotrygdBarnetrygdClientMock.finnesIkkeHosInfotrygd(any(), any()) } returns false

        val skalBehandlesHosInfotrygd = fødselshendelseService.fødselshendelseSkalBehandlesHosInfotrygd(søkerFnr, listOf(barn1Fnr))

        Assertions.assertTrue(skalBehandlesHosInfotrygd)
    }

    @Test
    fun `fødselshendelseSkalBehandlesHosInfotrygd skal filtrere bort aktørId`() {
        every { personopplysningerServiceMock.hentIdenter(Ident(søkerFnr)) } returns listOf(IdentInformasjon(søkerFnr, false, "FOLKEREGISTERIDENT"),
                                                                                         IdentInformasjon("1234567890123", false, "AKTORID"))
        every { personopplysningerServiceMock.hentIdenter(Ident(barn1Fnr)) } returns listOf(IdentInformasjon(barn1Fnr, false, "FOLKEREGISTERIDENT"))

        val slot = slot<List<String>>()
        every { infotrygdBarnetrygdClientMock.finnesIkkeHosInfotrygd(capture(slot), any()) } returns false

        fødselshendelseService.fødselshendelseSkalBehandlesHosInfotrygd(søkerFnr, listOf(barn1Fnr))

        Assertions.assertEquals(1, slot.captured.size)
        Assertions.assertEquals(søkerFnr, slot.captured[0])
    }

    @Test
    fun `fødselshendelseSkalBehandlesHosInfotrygd skal kollapse listen av barn til en samlet list av barn mot klienten`() {
        every { personopplysningerServiceMock.hentIdenter(Ident(søkerFnr)) } returns listOf(IdentInformasjon(søkerFnr, false, "FOLKEREGISTERIDENT"),
                IdentInformasjon("1234567890123", false, "AKTORID"))
        every { personopplysningerServiceMock.hentIdenter(Ident(barn1Fnr)) } returns listOf(IdentInformasjon(barn1Fnr, false, "FOLKEREGISTERIDENT"),
                IdentInformasjon("98765432101", false, "FOLKEREGISTERIDENT"))
        every { personopplysningerServiceMock.hentIdenter(Ident(barn2Fnr)) } returns listOf(IdentInformasjon(barn2Fnr, false, "FOLKEREGISTERIDENT"))

        val slot = slot<List<String>>()
        every { infotrygdBarnetrygdClientMock.finnesIkkeHosInfotrygd(any(), capture(slot)) } returns false

        fødselshendelseService.fødselshendelseSkalBehandlesHosInfotrygd(søkerFnr, listOf(barn1Fnr, barn2Fnr))

        Assertions.assertEquals(3, slot.captured.size)
    }

    @Test
    fun `Skal iverksette behandling hvis filtrering og vilkårsvurdering passerer og toggle er skrudd av`() {
        initMockk(vilkårsvurderingsResultat = BehandlingResultatType.INNVILGET)

        fødselshendelseService.opprettBehandlingOgKjørReglerForFødselshendelse(fødselshendelseBehandling)

        verify(exactly = 1) { IverksettMotOppdragTask.opprettTask(any(), any(), any()) }
        verify { OpprettOppgaveTask.opprettTask(any(), any(), any()) wasNot called }
    }


    @Test
    fun `Skal opprette oppgave hvis filtrering eller vilkårsvurdering gir avslag og toggle er skrudd av`() {
        initMockk(vilkårsvurderingsResultat = BehandlingResultatType.AVSLÅTT)

        fødselshendelseService.opprettBehandlingOgKjørReglerForFødselshendelse(fødselshendelseBehandling)

        verify(exactly = 1) { OpprettOppgaveTask.opprettTask(any(), any(), any()) }
        verify { IverksettMotOppdragTask.opprettTask(any(), any(), any()) wasNot called }
    }

    @Test
    fun `Skal kaste KontrollertRollbackException når toggle er skrudd på`() {

    }

    @Test
    fun `Skal ikke kjøre vilkårsvurdering når filtreringsregler gir avslag`() {

    }

    private fun initMockk(vilkårsvurderingsResultat: BehandlingResultatType) {
        val behandling = lagBehandling()
        val vedtak = lagVedtak(behandling = behandling)
        every { featureToggleServiceMock.isEnabled(any()) } returns false
        every { stegServiceMock.evaluerVilkårForFødselshendelse(any(), any()) } returns vilkårsvurderingsResultat
        every { stegServiceMock.opprettNyBehandlingOgRegistrerPersongrunnlagForHendelse(any()) } returns behandling
        every { evaluerFiltreringsreglerForFødselshendelseMock.evaluerFiltreringsregler(any(), any()) } returns Evaluering.ja("")
        every { vedtakServiceMock.hentAktivForBehandling(any()) } returns vedtak

        mockkObject(IverksettMotOppdragTask.Companion)
        every { IverksettMotOppdragTask.opprettTask(any(), any(), any()) } returns Task.nyTask(IverksettMotOppdragTask.TASK_STEP_TYPE, "")

        mockkObject(OpprettOppgaveTask.Companion)
        every { OpprettOppgaveTask.opprettTask(any(), any(), any()) } returns Task.nyTask(OpprettOppgaveTask.TASK_STEP_TYPE, "")
    }

    companion object {
        val fødselshendelseBehandling = NyBehandlingHendelse(morsIdent = "12345678910", barnasIdenter = listOf("01101800033"))
    }
}
