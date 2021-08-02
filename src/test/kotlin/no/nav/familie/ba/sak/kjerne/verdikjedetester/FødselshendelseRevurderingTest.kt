package no.nav.familie.ba.sak.kjerne.verdikjedetester

import io.mockk.every
import no.nav.familie.ba.sak.common.LocalDateService
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
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration
import java.time.LocalDate.now
import java.time.YearMonth
import java.util.concurrent.TimeUnit


class FødselshendelseRevurderingTest(
        @Autowired private val mockLocalDateService: LocalDateService
) : AbstractVerdikjedetest() {

    @Test
    fun `Skal innvilge fødselshendelse på mor med 1 barn med eksisterende utbetalinger`() {
        every { mockLocalDateService.now() } returns now().minusMonths(12) andThen now()

        val scenario = mockServerKlient().lagScenario(RestScenario(
                søker = RestScenarioPerson(fødselsdato = "1993-01-12", fornavn = "Mor", etternavn = "Søker"),
                barna = listOf(
                        RestScenarioPerson(fødselsdato = now().minusMonths(12).toString(),
                                           fornavn = "Barn",
                                           etternavn = "Barnesen"),
                        RestScenarioPerson(fødselsdato = now().minusDays(1).toString(),
                                           fornavn = "Barn2",
                                           etternavn = "Barnesen2")
                )
        ))

        val søkerIdent = scenario.søker.ident!!
        familieBaSakKlient().triggFødselshendelse(
                NyBehandlingHendelse(
                        morsIdent = søkerIdent,
                        barnasIdenter = listOf(scenario.barna.minByOrNull { it.fødselsdato }!!.ident!!)
                )
        )

        await.atMost(80, TimeUnit.SECONDS).withPollInterval(Duration.ofSeconds(1)).until {

            val fagsak =
                    familieBaSakKlient().hentFagsak(restHentFagsakForPerson = RestHentFagsakForPerson(personIdent = søkerIdent)).data
            println("FAGSAK ved fødselshendelse: $fagsak")
            fagsak?.status == FagsakStatus.LØPENDE && hentAktivBehandling(fagsak)?.steg == StegType.BEHANDLING_AVSLUTTET
        }

        val vurdertBarn = scenario.barna.maxByOrNull { it.fødselsdato }!!.ident!!
        familieBaSakKlient().triggFødselshendelse(
                NyBehandlingHendelse(
                        morsIdent = søkerIdent,
                        barnasIdenter = listOf(vurdertBarn)
                )
        )

        await.atMost(80, TimeUnit.SECONDS).withPollInterval(Duration.ofSeconds(1)).until {

            val fagsak =
                    familieBaSakKlient().hentFagsak(restHentFagsakForPerson = RestHentFagsakForPerson(personIdent = søkerIdent)).data
            println("FAGSAK ved fødselshendelse: $fagsak")
            fagsak?.status == FagsakStatus.LØPENDE && fagsak.behandlinger.size > 1 && hentAktivBehandling(fagsak)?.steg == StegType.BEHANDLING_AVSLUTTET
        }

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

        val utbetalingsperioder = aktivBehandling.utbetalingsperioder
        val gjeldendeUtbetalingsperiode =
                utbetalingsperioder.find { it.periodeFom.toYearMonth() == YearMonth.now().plusMonths(1) }!!

        assertUtbetalingsperiode(gjeldendeUtbetalingsperiode, 2, SatsService.tilleggOrdinærSatsNesteMånedTilTester.beløp * 2)
    }
}