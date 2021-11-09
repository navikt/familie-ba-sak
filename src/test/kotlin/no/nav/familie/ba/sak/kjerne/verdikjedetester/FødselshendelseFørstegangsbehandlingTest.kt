package no.nav.familie.ba.sak.kjerne.verdikjedetester

import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenarioPerson
import no.nav.familie.ba.sak.task.BehandleFødselshendelseTask
import no.nav.familie.kontrakter.felles.getDataOrThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class FødselshendelseFørstegangsbehandlingTest(
    @Autowired private val behandleFødselshendelseTask: BehandleFødselshendelseTask,
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val behandlingService: BehandlingService,
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val stegService: StegService
) : AbstractVerdikjedetest() {

    @Test
    fun `Skal innvilge fødselshendelse på mor med 1 barn uten utbetalinger`() {
        val scenario = mockServerKlient().lagScenario(
            RestScenario(
                søker = RestScenarioPerson(fødselsdato = "1996-01-12", fornavn = "Mor", etternavn = "Søker"),
                barna = listOf(
                    RestScenarioPerson(
                        fødselsdato = LocalDate.now().minusDays(2).toString(),
                        fornavn = "Barn",
                        etternavn = "Barnesen"
                    )
                )
            )
        )
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

        val restFagsakEtterBehandlingAvsluttet =
            familieBaSakKlient().hentFagsak(fagsakId = behandling!!.fagsak.id)
        generellAssertFagsak(
            restFagsak = restFagsakEtterBehandlingAvsluttet,
            fagsakStatus = FagsakStatus.LØPENDE,
            behandlingStegType = StegType.BEHANDLING_AVSLUTTET
        )

        val aktivBehandling = restFagsakEtterBehandlingAvsluttet.getDataOrThrow().behandlinger.single()
        assertEquals(BehandlingResultat.INNVILGET, aktivBehandling.resultat)

        val utbetalingsperioder = aktivBehandling.utbetalingsperioder

        val gjeldendeUtbetalingsperiode = utbetalingsperioder.find {
            it.periodeFom.toYearMonth() >= SatsService.tilleggOrdinærSatsNesteMånedTilTester.gyldigFom.toYearMonth() &&
                it.periodeFom.toYearMonth() <= SatsService.tilleggOrdinærSatsNesteMånedTilTester.gyldigTom.toYearMonth()
        }!!

        assertUtbetalingsperiode(
            gjeldendeUtbetalingsperiode,
            1,
            SatsService.tilleggOrdinærSatsNesteMånedTilTester.beløp * 1
        )
    }

    @Test
    fun `Skal innvilge fødselshendelse på mor med 2 barn uten utbetalinger`() {
        val scenario = mockServerKlient().lagScenario(
            RestScenario(
                søker = RestScenarioPerson(fødselsdato = "1996-01-12", fornavn = "Mor", etternavn = "Søker"),
                barna = listOf(
                    RestScenarioPerson(
                        fødselsdato = LocalDate.now().minusDays(2).toString(),
                        fornavn = "Barn",
                        etternavn = "Barnesen"
                    ),
                    RestScenarioPerson(
                        fødselsdato = LocalDate.now().minusDays(2).toString(),
                        fornavn = "Barn",
                        etternavn = "Barnesen 2"
                    )
                )
            )
        )
        val behandling = behandleFødselshendelse(
            nyBehandlingHendelse = NyBehandlingHendelse(
                morsIdent = scenario.søker.ident!!,
                barnasIdenter = scenario.barna.map { it.ident!! }
            ),
            behandleFødselshendelseTask = behandleFødselshendelseTask,
            fagsakService = fagsakService,
            behandlingService = behandlingService,
            vedtakService = vedtakService,
            stegService = stegService
        )

        val restFagsakEtterBehandlingAvsluttet =
            familieBaSakKlient().hentFagsak(fagsakId = behandling!!.fagsak.id)
        generellAssertFagsak(
            restFagsak = restFagsakEtterBehandlingAvsluttet,
            fagsakStatus = FagsakStatus.LØPENDE,
            behandlingStegType = StegType.BEHANDLING_AVSLUTTET
        )

        val aktivBehandling = restFagsakEtterBehandlingAvsluttet.getDataOrThrow().behandlinger.single()
        assertEquals(BehandlingResultat.INNVILGET, aktivBehandling.resultat)

        val utbetalingsperioder = aktivBehandling.utbetalingsperioder
        val gjeldendeUtbetalingsperiode = utbetalingsperioder.find {
            it.periodeFom.toYearMonth() >= SatsService.tilleggOrdinærSatsNesteMånedTilTester.gyldigFom.toYearMonth() &&
                it.periodeFom.toYearMonth() <= SatsService.tilleggOrdinærSatsNesteMånedTilTester.gyldigTom.toYearMonth()
        }!!

        assertUtbetalingsperiode(
            gjeldendeUtbetalingsperiode,
            2,
            SatsService.tilleggOrdinærSatsNesteMånedTilTester.beløp * 2
        )
    }
}
