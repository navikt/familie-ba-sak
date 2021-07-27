package no.nav.familie.ba.sak.kjerne.verdikjedetester

import io.mockk.mockk
import no.nav.familie.ba.sak.WebSpringAuthTestRunner
import no.nav.familie.ba.sak.ekstern.restDomene.RestHentFagsakForPerson
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.steg.StegType
import org.awaitility.kotlin.await
import org.awaitility.kotlin.withPollInterval
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.test.context.ActiveProfiles
import java.time.Duration
import java.time.LocalDate
import java.util.concurrent.TimeUnit

val scenarioFødselshendelseTest = Scenario(
        søker = ScenarioPerson(fødselsdato = LocalDate.parse("1996-01-12"), fornavn = "Mor", etternavn = "Søker"),
        barna = listOf(
                ScenarioPerson(fødselsdato = LocalDate.now().minusDays(2),
                               fornavn = "Barn",
                               etternavn = "Barnesen")
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
class FødselshendelseTest : WebSpringAuthTestRunner() {

    fun familieBaSakKlient(): FamilieBaSakKlient = FamilieBaSakKlient(
            baSakUrl = hentUrl(""),
            restOperations = restOperations,
            headers = hentHeadersForSystembruker()
    )

    @Test
    fun `Skal innvilge fødselshendelse på mor med 1 barn uten utbetalinger`() {
        familieBaSakKlient().triggFødselshendelse(
                NyBehandlingHendelse(
                        morsIdent = scenarioFødselshendelseTest.søker.personIdent,
                        barnasIdenter = scenarioFødselshendelseTest.barna.map { it.personIdent }
                )
        )

        await.atMost(80, TimeUnit.SECONDS).withPollInterval(Duration.ofSeconds(1)).until {

            val fagsak =
                    familieBaSakKlient().hentFagsak(restHentFagsakForPerson = RestHentFagsakForPerson(personIdent = scenarioFødselshendelseTest.søker.personIdent)).data
            println("FAGSAK ved fødselshendelse: $fagsak")
            fagsak?.status == FagsakStatus.LØPENDE
        }

        val restFagsakEtterBehandlingAvsluttet =
                familieBaSakKlient().hentFagsak(restHentFagsakForPerson = RestHentFagsakForPerson(personIdent = scenarioFødselshendelseTest.søker.personIdent))
        generellAssertFagsak(restFagsak = restFagsakEtterBehandlingAvsluttet,
                             fagsakStatus = FagsakStatus.LØPENDE,
                             behandlingStegType = StegType.BEHANDLING_AVSLUTTET)
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