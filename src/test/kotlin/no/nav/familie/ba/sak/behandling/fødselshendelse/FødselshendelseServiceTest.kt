package no.nav.familie.ba.sak.behandling.fødselshendelse

import io.mockk.*
import no.nav.familie.ba.sak.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.fødselshendelse.filtreringsregler.Fakta
import no.nav.familie.ba.sak.behandling.fødselshendelse.filtreringsregler.utfall.FiltreringsregelIkkeOppfylt.MOR_ER_UNDER_18_ÅR
import no.nav.familie.ba.sak.behandling.fødselshendelse.filtreringsregler.utfall.FiltreringsregelOppfylt.MOR_ER_OVER_18_ÅR
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.*
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultat
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatRepository
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.gdpr.GDPRService
import no.nav.familie.ba.sak.gdpr.domene.FødselshendelsePreLansering
import no.nav.familie.ba.sak.infotrygd.InfotrygdBarnetrygdClient
import no.nav.familie.ba.sak.infotrygd.InfotrygdFeedService
import no.nav.familie.ba.sak.nare.Evaluering
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.pdl.internal.IdentInformasjon
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.ba.sak.task.KontrollertRollbackException
import no.nav.familie.ba.sak.task.OpprettOppgaveTask
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class FødselshendelseServiceTest {

    private val infotrygdBarnetrygdClientMock = mockk<InfotrygdBarnetrygdClient>()
    private val personopplysningerServiceMock = mockk<PersonopplysningerService>()
    private val infotrygdFeedServiceMock = mockk<InfotrygdFeedService>()
    private val featureToggleServiceMock = mockk<FeatureToggleService>()
    private val stegServiceMock = mockk<StegService>()
    private val vedtakServiceMock = mockk<VedtakService>()
    private val evaluerFiltreringsreglerForFødselshendelseMock = mockk<EvaluerFiltreringsreglerForFødselshendelse>()
    private val taskRepositoryMock = mockk<TaskRepository>()
    private val behandlingResultatRepositoryMock = mockk<BehandlingResultatRepository>()
    private val persongrunnlagServiceMock = mockk<PersongrunnlagService>(relaxed = true)
    private val behandlingRepositoryMock = mockk<BehandlingRepository>()
    private val gdprServiceMock = mockk<GDPRService>()

    private val søkerFnr = "12345678910"
    private val barn1Fnr = "12345678911"
    private val barn2Fnr = "12345678912"

    private val fødselshendelseService = FødselshendelseService(infotrygdFeedServiceMock,
                                                                infotrygdBarnetrygdClientMock,
                                                                featureToggleServiceMock,
                                                                stegServiceMock,
                                                                vedtakServiceMock,
                                                                evaluerFiltreringsreglerForFødselshendelseMock,
                                                                taskRepositoryMock,
                                                                personopplysningerServiceMock,
                                                                behandlingResultatRepositoryMock,
                                                                persongrunnlagServiceMock,
                                                                behandlingRepositoryMock,
                                                                gdprServiceMock)

    @Test
    fun `fødselshendelseSkalBehandlesHosInfotrygd skal returne true dersom klienten returnerer false`() {
        every { personopplysningerServiceMock.hentIdenter(any()) } returns listOf(IdentInformasjon(søkerFnr,
                                                                                                   false,
                                                                                                   "FOLKEREGISTERIDENT"))
        every { infotrygdBarnetrygdClientMock.harIkkeLøpendeSakIInfotrygd(any(), any()) } returns false

        val skalBehandlesHosInfotrygd =
                fødselshendelseService.fødselshendelseSkalBehandlesHosInfotrygd(søkerFnr, listOf(barn1Fnr))

        Assertions.assertTrue(skalBehandlesHosInfotrygd)
    }

    @Test
    fun `fødselshendelseSkalBehandlesHosInfotrygd skal filtrere bort aktørId`() {
        every { personopplysningerServiceMock.hentIdenter(Ident(søkerFnr)) } returns listOf(IdentInformasjon(søkerFnr,
                                                                                                             false,
                                                                                                             "FOLKEREGISTERIDENT"),
                                                                                            IdentInformasjon("1234567890123",
                                                                                                             false,
                                                                                                             "AKTORID"))
        every { personopplysningerServiceMock.hentIdenter(Ident(barn1Fnr)) } returns listOf(IdentInformasjon(barn1Fnr,
                                                                                                             false,
                                                                                                             "FOLKEREGISTERIDENT"))

        val slot = slot<List<String>>()
        every { infotrygdBarnetrygdClientMock.harIkkeLøpendeSakIInfotrygd(capture(slot), any()) } returns false

        fødselshendelseService.fødselshendelseSkalBehandlesHosInfotrygd(søkerFnr, listOf(barn1Fnr))

        Assertions.assertEquals(1, slot.captured.size)
        Assertions.assertEquals(søkerFnr, slot.captured[0])
    }

    @Test
    fun `fødselshendelseSkalBehandlesHosInfotrygd skal kollapse listen av barn til en samlet list av barn mot klienten`() {
        every { personopplysningerServiceMock.hentIdenter(Ident(søkerFnr)) } returns listOf(IdentInformasjon(søkerFnr,
                                                                                                             false,
                                                                                                             "FOLKEREGISTERIDENT"),
                                                                                            IdentInformasjon("1234567890123",
                                                                                                             false,
                                                                                                             "AKTORID"))
        every { personopplysningerServiceMock.hentIdenter(Ident(barn1Fnr)) } returns listOf(IdentInformasjon(barn1Fnr,
                                                                                                             false,
                                                                                                             "FOLKEREGISTERIDENT"),
                                                                                            IdentInformasjon("98765432101",
                                                                                                             false,
                                                                                                             "FOLKEREGISTERIDENT"))
        every { personopplysningerServiceMock.hentIdenter(Ident(barn2Fnr)) } returns listOf(IdentInformasjon(barn2Fnr,
                                                                                                             false,
                                                                                                             "FOLKEREGISTERIDENT"))

        val slot = slot<List<String>>()
        every { infotrygdBarnetrygdClientMock.harIkkeLøpendeSakIInfotrygd(any(), capture(slot)) } returns false

        fødselshendelseService.fødselshendelseSkalBehandlesHosInfotrygd(søkerFnr, listOf(barn1Fnr, barn2Fnr))

        Assertions.assertEquals(3, slot.captured.size)
    }

    @Test
    fun `Skal iverksette behandling hvis filtrering og vilkårsvurdering passerer og toggle er skrudd av`() {
        initMockk(vilkårsvurderingsResultat = BehandlingResultatType.INNVILGET,
                  filtreringResultat = Evaluering.ja(MOR_ER_OVER_18_ÅR),
                  toggleVerdi = false)

        fødselshendelseService.opprettBehandlingOgKjørReglerForFødselshendelse(fødselshendelseBehandling)

        verify(exactly = 1) { IverksettMotOppdragTask.opprettTask(any(), any(), any()) }
        verify { OpprettOppgaveTask.opprettTask(any(), any(), any()) wasNot called }
    }


    @Test
    fun `Skal opprette oppgave hvis filtrering eller vilkårsvurdering gir avslag og toggle er skrudd av`() {
        initMockk(vilkårsvurderingsResultat = BehandlingResultatType.AVSLÅTT,
                  filtreringResultat = Evaluering.ja(MOR_ER_OVER_18_ÅR),
                  toggleVerdi = false)

        fødselshendelseService.opprettBehandlingOgKjørReglerForFødselshendelse(fødselshendelseBehandling)

        verify(exactly = 1) { OpprettOppgaveTask.opprettTask(any(), any(), any()) }
        verify { IverksettMotOppdragTask.opprettTask(any(), any(), any()) wasNot called }
    }

    @Test
    fun `Skal kaste KontrollertRollbackException når toggle er skrudd på`() {
        initMockk(vilkårsvurderingsResultat = BehandlingResultatType.INNVILGET,
                  filtreringResultat = Evaluering.ja(MOR_ER_OVER_18_ÅR),
                  toggleVerdi = true)

        assertThrows<KontrollertRollbackException> {
            fødselshendelseService.opprettBehandlingOgKjørReglerForFødselshendelse(fødselshendelseBehandling)
        }
        verify { IverksettMotOppdragTask.opprettTask(any(), any(), any()) wasNot called }
        verify { OpprettOppgaveTask.opprettTask(any(), any(), any()) wasNot called }
    }

    @Test
    fun `Skal ikke kjøre vilkårsvurdering og lage oppgave når filtreringsregler gir avslag`() {
        initMockk(vilkårsvurderingsResultat = BehandlingResultatType.INNVILGET,
                  filtreringResultat = Evaluering.nei(MOR_ER_UNDER_18_ÅR),
                  toggleVerdi = false)

        fødselshendelseService.opprettBehandlingOgKjørReglerForFødselshendelse(fødselshendelseBehandling)

        verify(exactly = 0) { stegServiceMock.evaluerVilkårForFødselshendelse(any(), any()) }
        verify(exactly = 1) { OpprettOppgaveTask.opprettTask(any(), any(), any()) }
        verify { IverksettMotOppdragTask.opprettTask(any(), any(), any()) wasNot called }
    }

    @Test
    fun `Skal iverksette behandling også for flerlinger hvis filtrering og vilkårsvurdering passerer og toggle er skrudd av`() {
        initMockk(vilkårsvurderingsResultat = BehandlingResultatType.INNVILGET,
                  filtreringResultat = Evaluering.ja(MOR_ER_OVER_18_ÅR),
                  toggleVerdi = false,
                  flerlinlinger = true)

        fødselshendelseService.opprettBehandlingOgKjørReglerForFødselshendelse(fødselshendelseFlerlingerBehandling)

        verify(exactly = 1) { IverksettMotOppdragTask.opprettTask(any(), any(), any()) }
        verify { OpprettOppgaveTask.opprettTask(any(), any(), any()) wasNot called }
    }

    private fun initMockk(vilkårsvurderingsResultat: BehandlingResultatType,
                          filtreringResultat: Evaluering,
                          toggleVerdi: Boolean,
                          flerlinlinger: Boolean = false) {

        val behandling = lagBehandling()
        val vedtak = lagVedtak(behandling = behandling)
        val opprettOppgaveTask = Task.nyTask(OpprettOppgaveTask.TASK_STEP_TYPE, "")
        val behandlingResultat = BehandlingResultat(behandling = behandling, personResultater = emptySet())
        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandling.id, personer = mutableSetOf())
        val søker = Person(type = PersonType.SØKER,
                           personIdent = PersonIdent("12345678910"),
                           fødselsdato = LocalDate.of(1990, 1, 12),
                           kjønn = Kjønn.KVINNE,
                           personopplysningGrunnlag = personopplysningGrunnlag,
                           sivilstand = SIVILSTAND.GIFT)
        val barna = listOf(Person(type = PersonType.BARN,
                                  personIdent = PersonIdent("01101800033"),
                                  fødselsdato = LocalDate.of(2018, 1, 12),
                                  kjønn = Kjønn.KVINNE,
                                  personopplysningGrunnlag = personopplysningGrunnlag,
                                  sivilstand = SIVILSTAND.UGIFT))
        if (flerlinlinger) barna.plus(Person(type = PersonType.BARN,
                                             personIdent = PersonIdent("01101800034"),
                                             fødselsdato = LocalDate.of(2018, 1, 12),
                                             kjønn = Kjønn.KVINNE,
                                             personopplysningGrunnlag = personopplysningGrunnlag,
                                             sivilstand = SIVILSTAND.UGIFT))

        personopplysningGrunnlag.personer.addAll(barna)
        personopplysningGrunnlag.personer.add(søker)

        every { featureToggleServiceMock.isEnabled(any()) } returns toggleVerdi
        every { stegServiceMock.evaluerVilkårForFødselshendelse(any(), any()) } returns vilkårsvurderingsResultat
        every { stegServiceMock.opprettNyBehandlingOgRegistrerPersongrunnlagForHendelse(any()) } returns behandling
        every {
            evaluerFiltreringsreglerForFødselshendelseMock.evaluerFiltreringsregler(any(),
                                                                                    any())
        } returns Pair(Fakta(mor = søker,
                             morHarVerge = false,
                             morLever = true,
                             barnetLever = true,
                             barnaFraHendelse = barna,
                             restenAvBarna = emptyList()), filtreringResultat)
        every { vedtakServiceMock.hentAktivForBehandling(any()) } returns vedtak
        every { vedtakServiceMock.oppdaterVedtakMedStønadsbrev(any()) } returns vedtak
        every { vedtakServiceMock.opprettVedtakOgTotrinnskontrollForAutomatiskBehandling(any()) } returns vedtak
        every { taskRepositoryMock.save(any()) } returns opprettOppgaveTask
        every { behandlingResultatRepositoryMock.findByBehandlingAndAktiv(any()) } returns behandlingResultat
        every { persongrunnlagServiceMock.hentSøker(any()) } returns søker
        every { persongrunnlagServiceMock.hentBarna(any()) } returns barna
        every { behandlingRepositoryMock.finnBehandling(any()) } returns behandling

        every { gdprServiceMock.lagreResultatAvFiltreringsregler(any(), any(), any(), any()) } just runs
        every { gdprServiceMock.hentFødselshendelsePreLansering(any()) } returns FødselshendelsePreLansering(personIdent = søker.personIdent.ident,
                                                                                                             behandlingId = behandling.id)

        mockkObject(IverksettMotOppdragTask.Companion)
        every {
            IverksettMotOppdragTask.opprettTask(any(),
                                                any(),
                                                any())
        } returns Task.nyTask(IverksettMotOppdragTask.TASK_STEP_TYPE, "")

        mockkObject(OpprettOppgaveTask.Companion)
        every { OpprettOppgaveTask.opprettTask(any(), any(), any()) } returns opprettOppgaveTask
    }

    companion object {

        val fødselshendelseBehandling = NyBehandlingHendelse(morsIdent = "12345678910", barnasIdenter = listOf("01101800033"))
        val fødselshendelseFlerlingerBehandling =
                NyBehandlingHendelse(morsIdent = "12345678910", barnasIdenter = listOf("01101800033", "01101800034"))
    }
}
