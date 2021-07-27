package no.nav.familie.ba.sak.kjerne.automatiskVurdering

import io.mockk.every
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.VergeResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.DødsfallData
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.IdentInformasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PersonInfo
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingIntegrationTest
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.GrBostedsadresseperiode
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.task.BehandleFødselshendelseTask
import no.nav.familie.ba.sak.task.OpprettOppgaveTask
import no.nav.familie.ba.sak.task.dto.BehandleFødselshendelseTaskDTO
import no.nav.familie.ba.sak.task.dto.OpprettOppgaveTaskDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import no.nav.familie.prosessering.domene.TaskRepository
import org.junit.Assert
import java.time.LocalDate


fun mockPersonopplysning(personnr: String, personInfo: PersonInfo, personopplysningerService: PersonopplysningerService) {
    every { personopplysningerService.hentPersoninfo(personnr) } returns personInfo
    every { personopplysningerService.hentIdenter(Ident(personnr)) } returns listOf(IdentInformasjon(personnr,
                                                                                                     false,
                                                                                                     gruppe = ""))
    every { personopplysningerService.hentPersoninfoMedRelasjoner(personnr) } returns personInfo
    every { personopplysningerService.hentAktivAktørId(Ident(personnr)) } returns AktørId(personnr)
    every { personopplysningerService.hentStatsborgerskap(Ident(personnr)) } returns
            listOf(Statsborgerskap("NOK",
                                   personInfo.fødselsdato,
                                   LocalDate.now()
                                           .plusYears(
                                                   30)))
    every {
        personopplysningerService.hentBostedsadresseperioder(personnr)
    } returns listOf(GrBostedsadresseperiode(
            periode = DatoIntervallEntitet(
                    fom = LocalDate.of(2002, 1, 4),
                    tom = LocalDate.of(2022, 1, 5)
            )))
    every { personopplysningerService.harVerge(any()) } returns VergeResponse(false)

    every {
        personopplysningerService.hentOpphold(personnr)
    } returns personInfo.opphold!!
    every { personopplysningerService.hentDødsfall(Ident(personnr)) } returns DødsfallData(false, null)

}

fun mockIntegrasjonsClient(personNr: String, integrasjonClient: IntegrasjonClient) {
    every { integrasjonClient.hentBehandlendeEnhet(personNr) } returns listOf(
            Arbeidsfordelingsenhet(enhetId = ArbeidsfordelingIntegrationTest.IKKE_FORTROLIG_ENHET,
                                   enhetNavn = "vanlig enhet"))
    every { integrasjonClient.hentLand(any()) } returns "NOK"
}

fun behandlingOgFagsakErÅpen(behanding: Behandling, fagsak: Fagsak?) {
    Assert.assertEquals(BehandlingStatus.UTREDES, behanding.status)
    Assert.assertEquals(BehandlingÅrsak.FØDSELSHENDELSE, behanding.opprettetÅrsak)
    Assert.assertEquals(StegType.FERDIGSTILLE_BEHANDLING, behanding.steg)
    Assert.assertEquals(FagsakStatus.LØPENDE, fagsak?.status)
}


fun lagOgkjørfødselshendelseTask(morsIdent: String,
                                 barnasIdenter: List<String>,
                                 behandleFødselshendelseTask: BehandleFødselshendelseTask) {
    val nyBehandlingHendelse = NyBehandlingHendelse(morsIdent, barnasIdenter)
    val task = BehandleFødselshendelseTask.opprettTask(BehandleFødselshendelseTaskDTO(nyBehandlingHendelse))
    behandleFødselshendelseTask.doTask(task)
}

fun løpendeFagsakForÅUnngåInfotrygd(morsIdent: String, fagsakService: FagsakService): Fagsak {
    val fagsak = fagsakService.hentEllerOpprettFagsak(PersonIdent(morsIdent), true)
    fagsakService.oppdaterStatus(fagsak, FagsakStatus.LØPENDE)
    return fagsak
}

fun hentDataForNyTask(taskRepository: TaskRepository, taskStepType: String = OpprettOppgaveTask.TASK_STEP_TYPE): OpprettOppgaveTaskDTO {
    val tasker = taskRepository.findAll()
    val taskForOpprettelseAvManuellBehandling = tasker.first {
        it.taskStepType == taskStepType
    }

    return objectMapper.readValue(taskForOpprettelseAvManuellBehandling.payload, OpprettOppgaveTaskDTO::class.java)
}