package no.nav.familie.ba.sak.kjerne.verdikjedetester

import io.mockk.every
import no.nav.familie.ba.sak.common.LocalDateService
import no.nav.familie.ba.sak.ekstern.restDomene.RestHentFagsakForPerson
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.autobrev.Autobrev6og18ÅrScheduler
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.steg.StegType
import org.awaitility.kotlin.await
import org.awaitility.kotlin.withPollInterval
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration
import java.time.LocalDate
import java.util.concurrent.TimeUnit

val scenarioBehandlingOmgjøringTest = Scenario(
        søker = ScenarioPerson(fødselsdato = LocalDate.parse("1993-01-12"), fornavn = "Mor", etternavn = "Søker"),
        barna = listOf(
                ScenarioPerson(fødselsdato = LocalDate.now().minusYears(6),
                               fornavn = "Barn",
                               etternavn = "Barnesen",
                               kjønn = Kjønn.KVINNE),
        )
).byggRelasjoner()


class BehandlingOmgjøringTest(
        @Autowired private val mockPersonopplysningerService: PersonopplysningerService,
        @Autowired private val mockLocalDateService: LocalDateService,
        @Autowired private val autobrev6og18ÅrScheduler: Autobrev6og18ÅrScheduler,
) : AbstractVerdikjedetest() {

    init {
        byggE2EPersonopplysningerServiceMock(mockPersonopplysningerService, scenarioBehandlingOmgjøringTest)
        every { mockLocalDateService.now() } returns LocalDate.now().minusYears(6) andThen LocalDate.now()
    }

    @Test
    fun `Skal innvilge fødselshendelse på mor med 1 barn med eksisterende utbetalinger`() {
        familieBaSakKlient().triggFødselshendelse(
                NyBehandlingHendelse(
                        morsIdent = scenarioBehandlingOmgjøringTest.søker.personIdent,
                        barnasIdenter = listOf(scenarioBehandlingOmgjøringTest.barna.first().personIdent)
                )
        )

        await.atMost(80, TimeUnit.SECONDS).withPollInterval(Duration.ofSeconds(1)).until {

            val fagsak =
                    familieBaSakKlient().hentFagsak(restHentFagsakForPerson = RestHentFagsakForPerson(personIdent = scenarioBehandlingOmgjøringTest.søker.personIdent)).data
            println("FAGSAK ved fødselshendelse omgjøring: $fagsak")
            fagsak?.status == FagsakStatus.LØPENDE && hentAktivBehandling(fagsak)?.steg == StegType.BEHANDLING_AVSLUTTET
        }

        autobrev6og18ÅrScheduler.opprettTask()

        await.atMost(80, TimeUnit.SECONDS).withPollInterval(Duration.ofSeconds(1)).until {

            val fagsak =
                    familieBaSakKlient().hentFagsak(restHentFagsakForPerson = RestHentFagsakForPerson(personIdent = scenarioBehandlingOmgjøringTest.søker.personIdent)).data
            println("FAGSAK ved omgjøring: $fagsak")
            fagsak?.status == FagsakStatus.LØPENDE && fagsak.behandlinger.firstOrNull { it.årsak == BehandlingÅrsak.OMREGNING_6ÅR }?.status == BehandlingStatus.AVSLUTTET
        }
    }
}