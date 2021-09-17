package no.nav.familie.ba.sak.kjerne.verdikjedetester

import io.mockk.every
import no.nav.familie.ba.sak.common.LocalDateService
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.ekstern.restDomene.RestHentFagsakForPerson
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
import java.time.LocalDate.now


class FødselshendelseRevurderingTest(
        @Autowired private val mockLocalDateService: LocalDateService,
        @Autowired private val behandleFødselshendelseTask: BehandleFødselshendelseTask,
        @Autowired private val fagsakService: FagsakService,
        @Autowired private val behandlingService: BehandlingService,
        @Autowired private val vedtakService: VedtakService,
        @Autowired private val stegService: StegService
) : AbstractVerdikjedetest() {

    @Test
    fun `Skal innvilge fødselshendelse på mor med 1 barn med eksisterende utbetalinger`() {
        every { mockLocalDateService.now() } returns now().minusMonths(12) andThen now()

        val revurderingsbarnSinFødselsdato = now().minusMonths(3)
        val scenario = mockServerKlient().lagScenario(RestScenario(
                søker = RestScenarioPerson(fødselsdato = "1993-01-12", fornavn = "Mor", etternavn = "Søker"),
                barna = listOf(
                        RestScenarioPerson(fødselsdato = now().minusMonths(12).toString(),
                                           fornavn = "Barn",
                                           etternavn = "Barnesen"),
                        RestScenarioPerson(fødselsdato = revurderingsbarnSinFødselsdato.toString(),
                                           fornavn = "Barn2",
                                           etternavn = "Barnesen2")
                )
        ))

        behandleFødselshendelse(
                nyBehandlingHendelse = NyBehandlingHendelse(
                        morsIdent = scenario.søker.ident!!,
                        barnasIdenter = listOf(scenario.barna.minByOrNull { it.fødselsdato }!!.ident!!)
                ),
                behandleFødselshendelseTask = behandleFødselshendelseTask,
                fagsakService = fagsakService,
                behandlingService = behandlingService,
                vedtakService = vedtakService,
                stegService = stegService
        )

        val søkerIdent = scenario.søker.ident
        val vurdertBarn = scenario.barna.maxByOrNull { it.fødselsdato }!!.ident!!
        behandleFødselshendelse(
                nyBehandlingHendelse = NyBehandlingHendelse(
                        morsIdent = søkerIdent,
                        barnasIdenter = listOf(vurdertBarn)
                ),
                fagsakStatusEtterVurdering = FagsakStatus.LØPENDE,
                behandleFødselshendelseTask = behandleFødselshendelseTask,
                fagsakService = fagsakService,
                behandlingService = behandlingService,
                vedtakService = vedtakService,
                stegService = stegService
        )

        val restFagsakEtterBehandlingAvsluttet =
                familieBaSakKlient().hentFagsak(restHentFagsakForPerson = RestHentFagsakForPerson(personIdent = søkerIdent))
        generellAssertFagsak(restFagsak = restFagsakEtterBehandlingAvsluttet,
                             fagsakStatus = FagsakStatus.LØPENDE,
                             behandlingStegType = StegType.BEHANDLING_AVSLUTTET)

        val aktivBehandling = restFagsakEtterBehandlingAvsluttet.getDataOrThrow().behandlinger.first { it.aktiv }
        val vurderteVilkårIDenneBehandlingen = aktivBehandling.personResultater.flatMap { it.vilkårResultater }
                .filter { it.behandlingId == aktivBehandling.behandlingId }
        assertEquals(BehandlingResultat.INNVILGET, aktivBehandling.resultat)
        assertEquals(5, vurderteVilkårIDenneBehandlingen.size)
        vurderteVilkårIDenneBehandlingen.forEach { assertEquals(revurderingsbarnSinFødselsdato, it.periodeFom) }

        val utbetalingsperioder = aktivBehandling.utbetalingsperioder
        val gjeldendeUtbetalingsperiode = utbetalingsperioder.find {
            it.periodeFom.toYearMonth() == SatsService.tilleggOrdinærSatsNesteMånedTilTester.gyldigFom.toYearMonth()
        }!!

        assertUtbetalingsperiode(gjeldendeUtbetalingsperiode, 2, SatsService.tilleggOrdinærSatsNesteMånedTilTester.beløp * 2)
    }
}