package no.nav.familie.ba.sak.kjerne.fødselshendelse

import io.mockk.called
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdBarnetrygdClient
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdFeedService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.IdentInformasjon
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.fødselshendelse.gdpr.GDPRService
import no.nav.familie.ba.sak.kjerne.fødselshendelse.gdpr.domene.FødselshendelsePreLansering
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
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

class FødselshendelseServiceDeprecatedTest {

    private val infotrygdBarnetrygdClientMock = mockk<InfotrygdBarnetrygdClient>()
    private val personopplysningerServiceMock = mockk<PersonopplysningerService>()
    private val infotrygdFeedServiceMock = mockk<InfotrygdFeedService>()
    private val envServiceMock = mockk<EnvService>()
    private val stegServiceMock = mockk<StegService>()
    private val vedtakServiceMock = mockk<VedtakService>()
    private val taskRepositoryMock = mockk<TaskRepository>()
    private val behandlingResultatRepositoryMock = mockk<VilkårsvurderingRepository>()
    private val persongrunnlagServiceMock = mockk<PersongrunnlagService>(relaxed = true)
    private val behandlingRepositoryMock = mockk<BehandlingRepository>()
    private val gdprServiceMock = mockk<GDPRService>()

    private val søkerFnr = "12345678910"
    private val barn1Fnr = "12345678911"
    private val barn2Fnr = "12345678912"

    private val fødselshendelseService = FødselshendelseServiceDeprecated(
            infotrygdFeedServiceMock,
            infotrygdBarnetrygdClientMock,
            stegServiceMock,
            vedtakServiceMock,
            taskRepositoryMock,
            personopplysningerServiceMock,
            behandlingResultatRepositoryMock,
            persongrunnlagServiceMock,
            behandlingRepositoryMock,
            gdprServiceMock,
            envServiceMock
    )

    @Test
    fun `fødselshendelseSkalBehandlesHosInfotrygd skal returne true dersom klienten returnerer true`() {
        every { personopplysningerServiceMock.hentIdenter(any()) } returns listOf(IdentInformasjon(søkerFnr,
                                                                                                   false,
                                                                                                   "FOLKEREGISTERIDENT"))

        every { infotrygdBarnetrygdClientMock.harLøpendeSakIInfotrygd(listOf("12345678910"), listOf("12345678910")) } returns true
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
        every { infotrygdBarnetrygdClientMock.harLøpendeSakIInfotrygd(capture(slot), listOf("12345678911")) } returns true

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
        every { infotrygdBarnetrygdClientMock.harLøpendeSakIInfotrygd(listOf("12345678910"), capture(slot)) } returns true

        fødselshendelseService.fødselshendelseSkalBehandlesHosInfotrygd(søkerFnr, listOf(barn1Fnr, barn2Fnr))

        Assertions.assertEquals(3, slot.captured.size)
    }

    @Test
    fun `Skal iverksette behandling hvis filtrering og vilkårsvurdering passerer og iverksetting er påskrudd`() {
        initMockk(behandlingResultat = BehandlingResultat.INNVILGET,
                  iverksettBehandling = true)

        fødselshendelseService.opprettBehandlingOgKjørReglerForFødselshendelse(fødselshendelseBehandling)

        verify(exactly = 1) { IverksettMotOppdragTask.opprettTask(any(), any(), any()) }
        verify { OpprettOppgaveTask.opprettTask(any(), any(), any()) wasNot called }

        uninitMockk()
    }


    @Test
    fun `Skal opprette oppgave hvis filtrering eller vilkårsvurdering gir avslag og iverksetting er påskrudd`() {
        initMockk(behandlingResultat = BehandlingResultat.AVSLÅTT,
                  iverksettBehandling = true)

        fødselshendelseService.opprettBehandlingOgKjørReglerForFødselshendelse(fødselshendelseBehandling)

        verify(exactly = 1) { OpprettOppgaveTask.opprettTask(any(), any(), any()) }
        verify { IverksettMotOppdragTask.opprettTask(any(), any(), any()) wasNot called }

        uninitMockk()
    }

    @Test
    fun `Skal kaste KontrollertRollbackException når iverksetting er avskrudd`() {
        initMockk(behandlingResultat = BehandlingResultat.INNVILGET,
                  iverksettBehandling = false)

        assertThrows<KontrollertRollbackException> {
            fødselshendelseService.opprettBehandlingOgKjørReglerForFødselshendelse(fødselshendelseBehandling)
        }
        verify { IverksettMotOppdragTask.opprettTask(any(), any(), any()) wasNot called }
        verify { OpprettOppgaveTask.opprettTask(any(), any(), any()) wasNot called }

        uninitMockk()
    }

    @Test
    fun `Skal iverksette behandling også for flerlinger hvis filtrering og vilkårsvurdering passerer og iverksetting er påskrudd`() {
        initMockk(behandlingResultat = BehandlingResultat.INNVILGET,
                  iverksettBehandling = true,
                  flerlinger = true)

        fødselshendelseService.opprettBehandlingOgKjørReglerForFødselshendelse(fødselshendelseFlerlingerBehandling)

        verify(exactly = 1) { IverksettMotOppdragTask.opprettTask(any(), any(), any()) }
        verify { OpprettOppgaveTask.opprettTask(any(), any(), any()) wasNot called }

        uninitMockk()
    }

    private fun initMockk(behandlingResultat: BehandlingResultat,
                          iverksettBehandling: Boolean,
                          flerlinger: Boolean = false) {

        val behandling = lagBehandling()
        val vedtak = lagVedtak(behandling = behandling)
        val opprettOppgaveTask = Task.nyTask(OpprettOppgaveTask.TASK_STEP_TYPE, "")
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling, personResultater = emptySet())
        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandling.id, personer = mutableSetOf())
        val søker = Person(type = PersonType.SØKER,
                           personIdent = PersonIdent("12345678910"),
                           fødselsdato = LocalDate.of(1990, 1, 12),
                           kjønn = Kjønn.KVINNE,
                           personopplysningGrunnlag = personopplysningGrunnlag)
                .apply { sivilstander = listOf(GrSivilstand(type = SIVILSTAND.GIFT, person = this)) }
        val barna = listOf(Person(type = PersonType.BARN,
                                  personIdent = PersonIdent("01101800033"),
                                  fødselsdato = LocalDate.of(2018, 1, 12),
                                  kjønn = Kjønn.KVINNE,
                                  personopplysningGrunnlag = personopplysningGrunnlag)
                                   .apply { sivilstander = listOf(GrSivilstand(type = SIVILSTAND.UGIFT, person = this)) })
        if (flerlinger) barna.plus(Person(type = PersonType.BARN,
                                          personIdent = PersonIdent("01101800034"),
                                          fødselsdato = LocalDate.of(2018, 1, 12),
                                          kjønn = Kjønn.KVINNE,
                                          personopplysningGrunnlag = personopplysningGrunnlag)
                                           .apply { sivilstander = listOf(GrSivilstand(type = SIVILSTAND.UGIFT, person = this)) })

        personopplysningGrunnlag.personer.addAll(barna)
        personopplysningGrunnlag.personer.add(søker)

        every { envServiceMock.skalIverksetteBehandling() } returns iverksettBehandling

        every { stegServiceMock.evaluerVilkårForFødselshendelse(any(), any()) } returns behandlingResultat
        every { stegServiceMock.opprettNyBehandlingOgRegistrerPersongrunnlagForHendelse(any()) } returns behandling
        every { vedtakServiceMock.hentAktivForBehandling(any()) } returns vedtak
        every { vedtakServiceMock.oppdaterVedtakMedStønadsbrev(any()) } returns vedtak
        every { vedtakServiceMock.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(any()) } returns vedtak
        every { taskRepositoryMock.save(any()) } returns opprettOppgaveTask
        every { behandlingResultatRepositoryMock.findByBehandlingAndAktiv(any()) } returns vilkårsvurdering
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

    fun uninitMockk() {
        unmockkObject(IverksettMotOppdragTask.Companion)
        unmockkObject(OpprettOppgaveTask.Companion)
    }

    companion object {

        val fødselshendelseBehandling = NyBehandlingHendelse(morsIdent = "12345678910", barnasIdenter = listOf("01101800033"))
        val fødselshendelseFlerlingerBehandling =
                NyBehandlingHendelse(morsIdent = "12345678910", barnasIdenter = listOf("01101800033", "01101800034"))
    }

}


