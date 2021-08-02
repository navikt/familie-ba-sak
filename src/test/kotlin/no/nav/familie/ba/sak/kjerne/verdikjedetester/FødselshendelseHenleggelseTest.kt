package no.nav.familie.ba.sak.kjerne.verdikjedetester

import io.mockk.every
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.ekstern.restDomene.RestHentFagsakForPerson
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdService
import no.nav.familie.ba.sak.kjerne.automatiskVurdering.hentDataForFørsteOpprettOppgaveTask
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.Resultat
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenarioPerson
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


class FødselshendelseHenleggelseTest(
        @Autowired private val taskRepository: TaskRepository,
        @Autowired private val infotrygdService: InfotrygdService
) : AbstractVerdikjedetest() {

    @Test
    fun `Skal ikke starte behandling i ba-sak fordi det finnes saker i infotrygd (velg fagsystem)`() {
        val scenario = mockServerKlient().lagScenario(RestScenario(
                søker = RestScenarioPerson(fødselsdato = "1982-01-12", fornavn = "Mor", etternavn = "Søker"),
                barna = listOf(
                        RestScenarioPerson(
                                fødselsdato = now().minusMonths(2).toString(),
                                fornavn = "Barn",
                                etternavn = "Barnesen",
                        )
                )
        ))
        every { infotrygdService.harLøpendeSakIInfotrygd(listOf(scenario.søker.ident!!)) } returns true

        familieBaSakKlient().triggFødselshendelse(
                NyBehandlingHendelse(
                        morsIdent = scenario.søker.ident!!,
                        barnasIdenter = listOf(scenario.barna.first().ident!!)
                )
        )

        await.atMost(80, TimeUnit.SECONDS).withPollInterval(Duration.ofSeconds(1)).until {

            val fagsak =
                    familieBaSakKlient().hentFagsak(restHentFagsakForPerson = RestHentFagsakForPerson(personIdent = scenario.søker.ident)).data
            val tasker = taskRepository.findAll()
            println("FAGSAK ved fødselshendelse velg fagsystem: $fagsak")
            fagsak == null && tasker.any { it.taskStepType == SendFeedTilInfotrygdTask.TASK_STEP_TYPE }
        }
    }

    @Test
    fun `Skal henlegge fødselshendelse på grunn av at barn krever etterbetaling (filtreringsregel)`() {
        val scenario = mockServerKlient().lagScenario(RestScenario(
                søker = RestScenarioPerson(fødselsdato = "1985-01-12", fornavn = "Mor", etternavn = "Søker"),
                barna = listOf(
                        RestScenarioPerson(
                                fødselsdato = now().minusMonths(2).toString(),
                                fornavn = "Barn",
                                etternavn = "Barnesen",
                        )
                )
        ))

        familieBaSakKlient().triggFødselshendelse(
                NyBehandlingHendelse(
                        morsIdent = scenario.søker.ident!!,
                        barnasIdenter = listOf(scenario.barna.first().ident!!)
                )
        )

        await.atMost(80, TimeUnit.SECONDS).withPollInterval(Duration.ofSeconds(1)).until {

            val fagsak =
                    familieBaSakKlient().hentFagsak(restHentFagsakForPerson = RestHentFagsakForPerson(personIdent = scenario.søker.ident)).data
            println("FAGSAK ved fødselshendelse: $fagsak")
            fagsak != null && fagsak.behandlinger.all { it.steg == StegType.BEHANDLING_AVSLUTTET }
        }

        val fagsak =
                familieBaSakKlient().hentFagsak(restHentFagsakForPerson = RestHentFagsakForPerson(personIdent = scenario.søker.ident)).data

        val automatiskVurdertBehandling = fagsak?.behandlinger?.first { it.skalBehandlesAutomatisk }!!
        assertEquals(0, automatiskVurdertBehandling.personResultater.size)

        val data = hentDataForFørsteOpprettOppgaveTask(taskRepository, automatiskVurdertBehandling.behandlingId)
        Assert.assertEquals("Saken medfører etterbetaling.",
                            data.beskrivelse)
    }

    @Test
    fun `Skal henlegge fødselshendelse på grunn av at barn ikke er bosatt i riket og bor ikke med mor (vilkårsvurdering)`() {
        val scenario = mockServerKlient().lagScenario(RestScenario(
                søker = RestScenarioPerson(fødselsdato = "1993-01-12", fornavn = "Mor", etternavn = "Søker"),
                barna = listOf(
                        RestScenarioPerson(fødselsdato = now().toString(),
                                           fornavn = "Barn",
                                           etternavn = "Barnesen",
                                           bostedsadresser = emptyList()
                        )
                )
        ))

        val søkerIdent = scenario.søker.ident!!
        val barnIdent = scenario.barna.first().ident!!
        familieBaSakKlient().triggFødselshendelse(
                NyBehandlingHendelse(
                        morsIdent = søkerIdent,
                        barnasIdenter = listOf(barnIdent)
                )
        )

        await.atMost(80, TimeUnit.SECONDS).withPollInterval(Duration.ofSeconds(1)).until {

            val fagsak =
                    familieBaSakKlient().hentFagsak(restHentFagsakForPerson = RestHentFagsakForPerson(personIdent = scenario.søker.ident)).data
            println("FAGSAK ved fødselshendelse: $fagsak")
            fagsak != null && fagsak.behandlinger.all { it.steg == StegType.BEHANDLING_AVSLUTTET }
        }

        val fagsak =
                familieBaSakKlient().hentFagsak(restHentFagsakForPerson = RestHentFagsakForPerson(personIdent = scenario.søker.ident)).data

        val automatiskVurdertBehandling = fagsak?.behandlinger?.first { it.skalBehandlesAutomatisk }!!
        val borMedSøkerVikårForbarn =
                automatiskVurdertBehandling.personResultater.firstOrNull { it.personIdent == barnIdent }?.vilkårResultater?.firstOrNull { it.vilkårType == Vilkår.BOR_MED_SØKER }
        val bosattIRiketVikårForbarn =
                automatiskVurdertBehandling.personResultater.firstOrNull { it.personIdent == barnIdent }?.vilkårResultater?.firstOrNull { it.vilkårType == Vilkår.BOSATT_I_RIKET }

        assertEquals(Resultat.IKKE_OPPFYLT, borMedSøkerVikårForbarn?.resultat)
        assertEquals(Resultat.IKKE_OPPFYLT, bosattIRiketVikårForbarn?.resultat)

        val data = hentDataForFørsteOpprettOppgaveTask(taskRepository, automatiskVurdertBehandling.behandlingId)
        Assert.assertEquals(automatiskVurdertBehandling.behandlingId, data.behandlingId)
        Assert.assertEquals("Barnet (fødselsdato: ${
            LocalDate.parse(scenario.barna.first().fødselsdato)
                    .tilKortString()
        }) er ikke bosatt med mor.",
                            data.beskrivelse)
    }
}