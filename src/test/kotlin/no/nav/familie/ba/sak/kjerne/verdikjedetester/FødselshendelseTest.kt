package no.nav.familie.ba.sak.kjerne.verdikjedetester

import io.mockk.mockk
import no.nav.familie.ba.sak.WebSpringAuthTestRunner
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.ekstern.restDomene.RestHentFagsakForPerson
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.kontrakter.felles.getDataOrThrow
import org.awaitility.kotlin.await
import org.awaitility.kotlin.withPollInterval
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.test.context.ActiveProfiles
import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth
import java.util.concurrent.TimeUnit

val scenarioFødselshendelseTest = Scenario(
        søker = ScenarioPerson(fødselsdato = LocalDate.parse("1996-01-12"), fornavn = "Mor", etternavn = "Søker"),
        barna = listOf(
                ScenarioPerson(fødselsdato = LocalDate.now().minusDays(2),
                               fornavn = "Barn",
                               etternavn = "Barnesen",
                               kjønn = Kjønn.KVINNE),
                ScenarioPerson(fødselsdato = LocalDate.now().minusDays(1),
                               fornavn = "Barn2",
                               etternavn = "Barnesen2",
                               kjønn = Kjønn.MANN)
        )
).byggRelasjoner()

@ActiveProfiles(
        "postgres",
        "mock-pdl-verdikjede-fødselshendelse-innvilget",
        "mock-oauth",
        "mock-arbeidsfordeling",
        "mock-tilbakekreving-klient",
        "mock-brev-klient",
        "mock-økonomi",
        "mock-infotrygd-feed",
        "mock-infotrygd-barnetrygd",
)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class FødselshendelseTest : WebSpringAuthTestRunner() {

    fun familieBaSakKlient(): FamilieBaSakKlient = FamilieBaSakKlient(
            baSakUrl = hentUrl(""),
            restOperations = restOperations,
            headers = hentHeadersForSystembruker()
    )

    @Test
    @Order(0)
    fun `Skal innvilge fødselshendelse på mor med 1 barn uten utbetalinger`() {
        familieBaSakKlient().triggFødselshendelse(
                NyBehandlingHendelse(
                        morsIdent = scenarioFødselshendelseTest.søker.personIdent,
                        barnasIdenter = listOf(scenarioFødselshendelseTest.barna.minByOrNull { it.fødselsdato }!!.personIdent)
                )
        )

        await.atMost(80, TimeUnit.SECONDS).withPollInterval(Duration.ofSeconds(1)).until {

            val fagsak =
                    familieBaSakKlient().hentFagsak(restHentFagsakForPerson = RestHentFagsakForPerson(personIdent = scenarioFødselshendelseTest.søker.personIdent)).data
            println("FAGSAK ved fødselshendelse: $fagsak")
            fagsak?.status == FagsakStatus.LØPENDE && hentAktivBehandling(fagsak)?.steg == StegType.BEHANDLING_AVSLUTTET
        }

        val restFagsakEtterBehandlingAvsluttet =
                familieBaSakKlient().hentFagsak(restHentFagsakForPerson = RestHentFagsakForPerson(personIdent = scenarioFødselshendelseTest.søker.personIdent))
        generellAssertFagsak(restFagsak = restFagsakEtterBehandlingAvsluttet,
                             fagsakStatus = FagsakStatus.LØPENDE,
                             behandlingStegType = StegType.BEHANDLING_AVSLUTTET)

        val aktivBehandling = restFagsakEtterBehandlingAvsluttet.getDataOrThrow().behandlinger.first { it.aktiv }
        assertEquals(BehandlingResultat.INNVILGET, aktivBehandling.resultat)

        val utbetalingsperioder = aktivBehandling.utbetalingsperioder
        val gjeldendeUtbetalingsperiode =
                utbetalingsperioder.find { it.periodeFom.toYearMonth() == YearMonth.now().plusMonths(1) }!!

        assertUtbetalingsperiode(gjeldendeUtbetalingsperiode, 1, SatsService.tilleggOrdinærSatsTilTester.beløp * 1)
    }

    @Test
    @Order(1)
    fun `Skal innvilge fødselshendelse på mor med 1 barn med eksisterende utbetalinger`() {
        val vurdertBarn = scenarioFødselshendelseTest.barna.maxByOrNull { it.fødselsdato }!!.personIdent
        val ikkeVurdertBarn = scenarioFødselshendelseTest.barna.minByOrNull { it.fødselsdato }!!.personIdent
        familieBaSakKlient().triggFødselshendelse(
                NyBehandlingHendelse(
                        morsIdent = scenarioFødselshendelseTest.søker.personIdent,
                        barnasIdenter = listOf(vurdertBarn)
                )
        )

        await.atMost(80, TimeUnit.SECONDS).withPollInterval(Duration.ofSeconds(1)).until {

            val fagsak =
                    familieBaSakKlient().hentFagsak(restHentFagsakForPerson = RestHentFagsakForPerson(personIdent = scenarioFødselshendelseTest.søker.personIdent)).data
            println("FAGSAK ved fødselshendelse: $fagsak")
            fagsak?.status == FagsakStatus.LØPENDE && fagsak.behandlinger.size > 1 && hentAktivBehandling(fagsak)?.steg == StegType.BEHANDLING_AVSLUTTET
        }

        val restFagsakEtterBehandlingAvsluttet =
                familieBaSakKlient().hentFagsak(restHentFagsakForPerson = RestHentFagsakForPerson(personIdent = scenarioFødselshendelseTest.søker.personIdent))
        generellAssertFagsak(restFagsak = restFagsakEtterBehandlingAvsluttet,
                             fagsakStatus = FagsakStatus.LØPENDE,
                             behandlingStegType = StegType.BEHANDLING_AVSLUTTET)

        val aktivBehandling = restFagsakEtterBehandlingAvsluttet.getDataOrThrow().behandlinger.first { it.aktiv }
        assertEquals(BehandlingResultat.INNVILGET, aktivBehandling.resultat)
        assertTrue(aktivBehandling.personResultater.none { it.vilkårResultater.any { restVilkårResultat -> it.personIdent == ikkeVurdertBarn && restVilkårResultat.behandlingId == aktivBehandling.behandlingId } })

        val utbetalingsperioder = aktivBehandling.utbetalingsperioder
        val gjeldendeUtbetalingsperiode =
                utbetalingsperioder.find { it.periodeFom.toYearMonth() == YearMonth.now().plusMonths(1) }!!

        assertUtbetalingsperiode(gjeldendeUtbetalingsperiode, 2, SatsService.tilleggOrdinærSatsTilTester.beløp * 2)
    }
}

@Configuration
class E2ETestConfigurationFødselshendelseTest {

    @Bean
    @Profile("mock-pdl-verdikjede-fødselshendelse-innvilget")
    @Primary
    fun mockPersonopplysningerService(): PersonopplysningerService {
        val mockPersonopplysningerService = mockk<PersonopplysningerService>(relaxed = false)

        return byggE2EPersonopplysningerServiceMock(mockPersonopplysningerService, scenarioFødselshendelseTest)
    }
}