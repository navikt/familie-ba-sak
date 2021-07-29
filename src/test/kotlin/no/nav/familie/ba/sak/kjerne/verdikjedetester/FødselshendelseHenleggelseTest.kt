package no.nav.familie.ba.sak.kjerne.verdikjedetester

import io.mockk.every
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.ekstern.restDomene.RestHentFagsakForPerson
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.automatiskVurdering.hentDataForFørsteOpprettOppgaveTask
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.Vilkår
import no.nav.familie.ba.sak.task.SendFeedTilInfotrygdTask
import no.nav.familie.prosessering.domene.TaskRepository
import org.awaitility.kotlin.await
import org.awaitility.kotlin.withPollInterval
import org.junit.Assert
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDate.now
import java.util.concurrent.TimeUnit

val scenarioFødselshendelseHenleggelseMorMedLøpendeSakIInfotrygd = Scenario(
        søker = ScenarioPerson(fødselsdato = LocalDate.parse("1982-01-12"), fornavn = "Mor", etternavn = "Søker"),
        barna = listOf(
                ScenarioPerson(
                        fødselsdato = now().minusMonths(2),
                        fornavn = "Barn",
                        etternavn = "Barnesen",
                        kjønn = Kjønn.KVINNE,
                )
        )
).byggRelasjoner()

val scenarioFødselshendelseHenleggelseBarnMedførerEtterbetaling = Scenario(
        søker = ScenarioPerson(fødselsdato = LocalDate.parse("1985-01-12"), fornavn = "Mor", etternavn = "Søker"),
        barna = listOf(
                ScenarioPerson(
                        fødselsdato = now().minusMonths(2),
                        fornavn = "Barn",
                        etternavn = "Barnesen",
                        kjønn = Kjønn.KVINNE,
                )
        )
).byggRelasjoner()

val scenarioFødselshendelseHenleggelseBarnUtenAdresse = Scenario(
        søker = ScenarioPerson(fødselsdato = LocalDate.parse("1993-01-12"), fornavn = "Mor", etternavn = "Søker"),
        barna = listOf(
                ScenarioPerson(fødselsdato = now(),
                               fornavn = "Barn",
                               etternavn = "Barnesen",
                               kjønn = Kjønn.KVINNE,
                               adresser = emptyList()
                )
        )
).byggRelasjoner()

class FødselshendelseHenleggelseTest(
        @Autowired private val mockPersonopplysningerService: PersonopplysningerService,
        @Autowired private val taskRepository: TaskRepository,
        @Autowired private val infotrygdService: InfotrygdService
) : AbstractVerdikjedetest() {

    fun familieBaSakKlient(): FamilieBaSakKlient = FamilieBaSakKlient(
            baSakUrl = hentUrl(""),
            restOperations = restOperations,
            headers = hentHeadersForSystembruker()
    )

    @Test
    fun `Skal ikke starte behandling i ba-sak fordi det finnes saker i infotrygd (velg fagsystem)`() {
        byggE2EPersonopplysningerServiceMock(mockPersonopplysningerService,
                                             scenarioFødselshendelseHenleggelseMorMedLøpendeSakIInfotrygd)
        every { infotrygdService.harLøpendeSakIInfotrygd(listOf(scenarioFødselshendelseHenleggelseMorMedLøpendeSakIInfotrygd.søker.personIdent)) } returns true

        familieBaSakKlient().triggFødselshendelse(
                NyBehandlingHendelse(
                        morsIdent = scenarioFødselshendelseHenleggelseMorMedLøpendeSakIInfotrygd.søker.personIdent,
                        barnasIdenter = listOf(scenarioFødselshendelseHenleggelseMorMedLøpendeSakIInfotrygd.barna.first().personIdent)
                )
        )

        await.atMost(80, TimeUnit.SECONDS).withPollInterval(Duration.ofSeconds(1)).until {

            val fagsak =
                    familieBaSakKlient().hentFagsak(restHentFagsakForPerson = RestHentFagsakForPerson(personIdent = scenarioFødselshendelseHenleggelseMorMedLøpendeSakIInfotrygd.søker.personIdent)).data
            val tasker = taskRepository.findAll()
            println("FAGSAK ved fødselshendelse velg fagsystem: $fagsak")
            fagsak == null && tasker.any { it.taskStepType == SendFeedTilInfotrygdTask.TASK_STEP_TYPE }
        }
    }

    @Test
    fun `Skal henlegge fødselshendelse på grunn av at barn krever etterbetaling (filtreringsregel)`() {
        byggE2EPersonopplysningerServiceMock(mockPersonopplysningerService,
                                             scenarioFødselshendelseHenleggelseBarnMedførerEtterbetaling)

        familieBaSakKlient().triggFødselshendelse(
                NyBehandlingHendelse(
                        morsIdent = scenarioFødselshendelseHenleggelseBarnMedførerEtterbetaling.søker.personIdent,
                        barnasIdenter = listOf(scenarioFødselshendelseHenleggelseBarnMedførerEtterbetaling.barna.first().personIdent)
                )
        )

        await.atMost(80, TimeUnit.SECONDS).withPollInterval(Duration.ofSeconds(1)).until {

            val fagsak =
                    familieBaSakKlient().hentFagsak(restHentFagsakForPerson = RestHentFagsakForPerson(personIdent = scenarioFødselshendelseHenleggelseBarnMedførerEtterbetaling.søker.personIdent)).data
            println("FAGSAK ved fødselshendelse: $fagsak")
            fagsak != null && fagsak.behandlinger.all { it.steg == StegType.BEHANDLING_AVSLUTTET }
        }

        val fagsak =
                familieBaSakKlient().hentFagsak(restHentFagsakForPerson = RestHentFagsakForPerson(personIdent = scenarioFødselshendelseHenleggelseBarnMedførerEtterbetaling.søker.personIdent)).data

        val automatiskVurdertBehandling = fagsak?.behandlinger?.first { it.skalBehandlesAutomatisk }!!
        assertEquals(0, automatiskVurdertBehandling.personResultater.size)

        val data = hentDataForFørsteOpprettOppgaveTask(taskRepository, automatiskVurdertBehandling.behandlingId)
        Assert.assertEquals("Saken medfører etterbetaling.",
                            data.beskrivelse)
    }

    @Test
    fun `Skal henlegge fødselshendelse på grunn av at barn ikke er bosatt i riket og bor ikke med mor (vilkårsvurdering)`() {
        byggE2EPersonopplysningerServiceMock(mockPersonopplysningerService, scenarioFødselshendelseHenleggelseBarnUtenAdresse)

        familieBaSakKlient().triggFødselshendelse(
                NyBehandlingHendelse(
                        morsIdent = scenarioFødselshendelseHenleggelseBarnUtenAdresse.søker.personIdent,
                        barnasIdenter = listOf(scenarioFødselshendelseHenleggelseBarnUtenAdresse.barna.first().personIdent)
                )
        )

        await.atMost(80, TimeUnit.SECONDS).withPollInterval(Duration.ofSeconds(1)).until {

            val fagsak =
                    familieBaSakKlient().hentFagsak(restHentFagsakForPerson = RestHentFagsakForPerson(personIdent = scenarioFødselshendelseHenleggelseBarnUtenAdresse.søker.personIdent)).data
            println("FAGSAK ved fødselshendelse: $fagsak")
            fagsak != null && fagsak.behandlinger.all { it.steg == StegType.BEHANDLING_AVSLUTTET }
        }

        val fagsak =
                familieBaSakKlient().hentFagsak(restHentFagsakForPerson = RestHentFagsakForPerson(personIdent = scenarioFødselshendelseHenleggelseBarnUtenAdresse.søker.personIdent)).data

        val automatiskVurdertBehandling = fagsak?.behandlinger?.first { it.skalBehandlesAutomatisk }!!
        val borMedSøkerVikårForbarn =
                automatiskVurdertBehandling.personResultater.firstOrNull { it.personIdent == scenarioFødselshendelseHenleggelseBarnUtenAdresse.barna.first().personIdent }?.vilkårResultater?.firstOrNull { it.vilkårType == Vilkår.BOR_MED_SØKER }
        val bosattIRiketVikårForbarn =
                automatiskVurdertBehandling.personResultater.firstOrNull { it.personIdent == scenarioFødselshendelseHenleggelseBarnUtenAdresse.barna.first().personIdent }?.vilkårResultater?.firstOrNull { it.vilkårType == Vilkår.BOSATT_I_RIKET }

        assertEquals(Resultat.IKKE_OPPFYLT, borMedSøkerVikårForbarn?.resultat)
        assertEquals(Resultat.IKKE_OPPFYLT, bosattIRiketVikårForbarn?.resultat)

        val data = hentDataForFørsteOpprettOppgaveTask(taskRepository, automatiskVurdertBehandling.behandlingId)
        Assert.assertEquals(automatiskVurdertBehandling.behandlingId, data.behandlingId)
        Assert.assertEquals("Barnet (fødselsdato: ${scenarioFødselshendelseHenleggelseBarnUtenAdresse.barna.first().fødselsdato.tilKortString()}) er ikke bosatt med mor.",
                            data.beskrivelse)
    }
}