package no.nav.familie.ba.sak.kjerne.verdikjedetester

import io.mockk.every
import io.mockk.verify
import no.nav.familie.ba.sak.common.LocalDateService
import no.nav.familie.ba.sak.kjerne.autobrev.Autobrev6og18ÅrService
import no.nav.familie.ba.sak.kjerne.autobrev.FinnAlleBarn6og18ÅrTask
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenarioPerson
import no.nav.familie.ba.sak.task.BehandleFødselshendelseTask
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.ba.sak.task.dto.Autobrev6og18ÅrDTO
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Matrikkeladresse
import no.nav.familie.prosessering.domene.Task
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.YearMonth

class BehandlingOmgjøringTest(
        @Autowired private val mockLocalDateService: LocalDateService,
        @Autowired private val autobrev6og18ÅrService: Autobrev6og18ÅrService,
        @Autowired private val finnAlleBarn6og18ÅrTask: FinnAlleBarn6og18ÅrTask,
        @Autowired private val behandleFødselshendelseTask: BehandleFødselshendelseTask,
        @Autowired private val fagsakService: FagsakService,
        @Autowired private val behandlingService: BehandlingService,
        @Autowired private val vedtakService: VedtakService,
        @Autowired private val stegService: StegService,
        @Autowired private val opprettTaskService: OpprettTaskService
) : AbstractVerdikjedetest() {

    @Test
    fun `Skal innvilge fødselshendelse på mor med 1 barn med eksisterende utbetalinger`() {
        every { mockLocalDateService.now() } returns LocalDate.now().minusYears(6) andThen LocalDate.now()

        val scenario = mockServerKlient().lagScenario(RestScenario(
                søker = RestScenarioPerson(fødselsdato = "1993-01-12", fornavn = "Mor", etternavn = "Søker").copy(
                        bostedsadresser = mutableListOf(Bostedsadresse(angittFlyttedato = LocalDate.now().minusYears(10),
                                                                       gyldigTilOgMed = null,
                                                                       matrikkeladresse = Matrikkeladresse(matrikkelId = 123L,
                                                                                                           bruksenhetsnummer = "H301",
                                                                                                           tilleggsnavn = "navn",
                                                                                                           postnummer = "0202",
                                                                                                           kommunenummer = "2231")))),
                barna = listOf(
                        RestScenarioPerson(fødselsdato = LocalDate.now()
                                .minusYears(6)
                                .toString(),
                                           fornavn = "Barn",
                                           etternavn = "Barnesen").copy(
                                bostedsadresser = mutableListOf(Bostedsadresse(angittFlyttedato = LocalDate.now().minusYears(6),
                                                                               gyldigTilOgMed = null,
                                                                               matrikkeladresse = Matrikkeladresse(matrikkelId = 123L,
                                                                                                                   bruksenhetsnummer = "H301",
                                                                                                                   tilleggsnavn = "navn",
                                                                                                                   postnummer = "0202",
                                                                                                                   kommunenummer = "2231")))),
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
        )!!

        finnAlleBarn6og18ÅrTask.doTask(
                Task(
                        type = FinnAlleBarn6og18ÅrTask.TASK_STEP_TYPE,
                        payload = ""
                )
        )
        verify(exactly = 1) { opprettTaskService.opprettAutovedtakFor6Og18ÅrBarn(behandling.fagsak.id, 6) }

        autobrev6og18ÅrService.opprettOmregningsoppgaveForBarnIBrytingsalder(
                autobrev6og18ÅrDTO = Autobrev6og18ÅrDTO(
                        fagsakId = behandling.fagsak.id,
                        alder = scenario.barna.first().alder,
                        årMåned = YearMonth.now()
                )
        )
        val omgjøringsbehandling = behandlingService.hentAktivForFagsak(fagsakId = behandling.fagsak.id)
        assertEquals(BehandlingResultat.FORTSATT_INNVILGET, omgjøringsbehandling?.resultat)
        assertEquals(StegType.JOURNALFØR_VEDTAKSBREV, omgjøringsbehandling?.steg)

        val omgjøringsvedtak = vedtakService.hentAktivForBehandling(behandlingId = omgjøringsbehandling!!.id)
        assertNotNull(omgjøringsvedtak)
    }
}