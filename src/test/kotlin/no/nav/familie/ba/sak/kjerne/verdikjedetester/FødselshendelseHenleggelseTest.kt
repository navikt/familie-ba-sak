package no.nav.familie.ba.sak.kjerne.verdikjedetester

import io.mockk.verify
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.ekstern.restDomene.RestHentFagsakForPerson
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenarioPerson
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.task.BehandleFødselshendelseTask
import no.nav.familie.kontrakter.ba.infotrygd.InfotrygdSøkResponse
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDate.now

class FødselshendelseHenleggelseTest(
        @Autowired private val opprettTaskService: OpprettTaskService,
        @Autowired private val behandleFødselshendelseTask: BehandleFødselshendelseTask,
        @Autowired private val fagsakService: FagsakService,
        @Autowired private val behandlingService: BehandlingService,
        @Autowired private val vedtakService: VedtakService,
        @Autowired private val stegService: StegService
) : AbstractVerdikjedetest() {

    @Test
    fun `Skal ikke starte behandling i ba-sak fordi det finnes saker i infotrygd (velg fagsystem)`() {
        val scenario = mockServerKlient().lagScenario(RestScenario(
                søker = RestScenarioPerson(
                        fødselsdato = "1982-01-12",
                        fornavn = "Mor",
                        etternavn = "Søker",
                        infotrygdSaker = InfotrygdSøkResponse(
                                bruker = listOf(
                                        lagInfotrygdSak(
                                                SatsService.nyttTilleggOrdinærSats.beløp.toDouble(),
                                                "1234",
                                                "OR",
                                                "OS"
                                        )
                                ),
                                barn = emptyList()
                        )
                ),
                barna = listOf(
                        RestScenarioPerson(
                                fødselsdato = now().minusMonths(2).toString(),
                                fornavn = "Barn",
                                etternavn = "Barnesen",
                        )
                )
        ))

        val behandling = behandleFødselshendelse(
                nyBehandlingHendelse = NyBehandlingHendelse(
                        morsIdent = scenario.søker.ident!!,
                        barnasIdenter = listOf(scenario.barna.first().ident!!)
                ),
                behandleFødselshendelseTask = behandleFødselshendelseTask,
                fagsakService = fagsakService,
                behandlingService = behandlingService,
                vedtakService = vedtakService,
                stegService = stegService
        )
        assertNull(behandling)

        verify(exactly = 1) {
            opprettTaskService.opprettSendFeedTilInfotrygdTask(scenario.barna.map { it.ident!! })
        }
    }

    @Test
    fun `Skal henlegge fødselshendelse på grunn av at søker er under 18 (filtreringsregel)`() {
        val scenario = mockServerKlient().lagScenario(RestScenario(
                søker = RestScenarioPerson(fødselsdato = now().minusYears(16).toString(), fornavn = "Mor", etternavn = "Søker"),
                barna = listOf(
                        RestScenarioPerson(
                                fødselsdato = now().minusMonths(2).toString(),
                                fornavn = "Barn",
                                etternavn = "Barnesen",
                        )
                )
        ))

        val behandling = behandleFødselshendelse(
                nyBehandlingHendelse = NyBehandlingHendelse(
                        morsIdent = scenario.søker.ident!!,
                        barnasIdenter = listOf(scenario.barna.first().ident!!)
                ),
                behandleFødselshendelseTask = behandleFødselshendelseTask,
                fagsakService = fagsakService,
                behandlingService = behandlingService,
                vedtakService = vedtakService,
                stegService = stegService
        )

        assertEquals(BehandlingResultat.HENLAGT_AUTOMATISK_FØDSELSHENDELSE, behandling?.resultat)
        assertEquals(StegType.BEHANDLING_AVSLUTTET, behandling?.steg)

        verify(exactly = 1) {
            opprettTaskService.opprettOppgaveTask(
                    behandlingId = behandling!!.id,
                    oppgavetype = Oppgavetype.VurderLivshendelse,
                    beskrivelse = "Mor er under 18 år."
            )
        }

        val fagsak =
                familieBaSakKlient().hentFagsak(restHentFagsakForPerson = RestHentFagsakForPerson(personIdent = scenario.søker.ident)).data

        val automatiskVurdertBehandling = fagsak?.behandlinger?.first { it.skalBehandlesAutomatisk }!!
        assertEquals(0, automatiskVurdertBehandling.personResultater.size)
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

        val barnIdent = scenario.barna.first().ident!!
        val behandling = behandleFødselshendelse(
                nyBehandlingHendelse = NyBehandlingHendelse(
                        morsIdent = scenario.søker.ident!!,
                        barnasIdenter = listOf(scenario.barna.first().ident!!)
                ),
                behandleFødselshendelseTask = behandleFødselshendelseTask,
                fagsakService = fagsakService,
                behandlingService = behandlingService,
                vedtakService = vedtakService,
                stegService = stegService
        )

        assertEquals(BehandlingResultat.HENLAGT_AUTOMATISK_FØDSELSHENDELSE, behandling?.resultat)
        assertEquals(StegType.BEHANDLING_AVSLUTTET, behandling?.steg)

        verify(exactly = 1) {
            opprettTaskService.opprettOppgaveTask(
                    behandlingId = behandling!!.id,
                    oppgavetype = Oppgavetype.VurderLivshendelse,
                    beskrivelse = "Barnet (fødselsdato: ${
                        LocalDate.parse(scenario.barna.first().fødselsdato)
                                .tilKortString()
                    }) er ikke bosatt med mor."
            )
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
    }
}