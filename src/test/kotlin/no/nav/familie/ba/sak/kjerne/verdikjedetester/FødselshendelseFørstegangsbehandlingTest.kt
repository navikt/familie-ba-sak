package no.nav.familie.ba.sak.kjerne.verdikjedetester

import no.nav.familie.ba.sak.common.convertDataClassToJson
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.ekstern.restDomene.RestHentFagsakForPerson
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenarioPerson
import no.nav.familie.kontrakter.felles.getDataOrThrow
import org.awaitility.kotlin.await
import org.awaitility.kotlin.withPollInterval
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth
import java.util.concurrent.TimeUnit

class FødselshendelseFørstegangsbehandlingTest : AbstractVerdikjedetest() {

    @Test
    fun `Skal innvilge fødselshendelse på mor med 1 barn uten utbetalinger`() {
        val scenario = mockServerKlient().lagScenario(RestScenario(
                søker = RestScenarioPerson(fødselsdato = "1996-01-12", fornavn = "Mor", etternavn = "Søker"),
                barna = listOf(
                        RestScenarioPerson(fødselsdato = LocalDate.now().minusDays(2).toString(),
                                           fornavn = "Barn",
                                           etternavn = "Barnesen")
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
            println("FAGSAK ved fødselshendelse: ${fagsak?.convertDataClassToJson()}")
            fagsak?.status == FagsakStatus.LØPENDE && hentAktivBehandling(fagsak)?.steg == StegType.BEHANDLING_AVSLUTTET
        }

        val restFagsakEtterBehandlingAvsluttet =
                familieBaSakKlient().hentFagsak(restHentFagsakForPerson = RestHentFagsakForPerson(personIdent = scenario.søker.ident))
        generellAssertFagsak(restFagsak = restFagsakEtterBehandlingAvsluttet,
                             fagsakStatus = FagsakStatus.LØPENDE,
                             behandlingStegType = StegType.BEHANDLING_AVSLUTTET)

        val aktivBehandling = restFagsakEtterBehandlingAvsluttet.getDataOrThrow().behandlinger.first { it.aktiv }
        assertEquals(BehandlingResultat.INNVILGET, aktivBehandling.resultat)

        val utbetalingsperioder = aktivBehandling.utbetalingsperioder
        val gjeldendeUtbetalingsperiode =
                utbetalingsperioder.find { it.periodeFom.toYearMonth() == YearMonth.now().plusMonths(1) }!!

        assertUtbetalingsperiode(gjeldendeUtbetalingsperiode, 1, SatsService.tilleggOrdinærSatsNesteMånedTilTester.beløp * 1)
    }
}
