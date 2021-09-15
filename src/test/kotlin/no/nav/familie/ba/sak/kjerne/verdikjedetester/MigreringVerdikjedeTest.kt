package no.nav.familie.ba.sak.kjerne.verdikjedetester

import no.nav.familie.ba.sak.kjerne.beregning.SatsService.nyttTilleggOrdinærSats
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenarioPerson
import no.nav.familie.kontrakter.ba.infotrygd.Barn
import no.nav.familie.kontrakter.ba.infotrygd.Delytelse
import no.nav.familie.kontrakter.ba.infotrygd.InfotrygdSøkResponse
import no.nav.familie.kontrakter.ba.infotrygd.Sak
import no.nav.familie.kontrakter.ba.infotrygd.Stønad
import org.assertj.core.api.Assertions
import org.awaitility.core.ConditionTimeoutException
import org.awaitility.kotlin.await
import org.awaitility.kotlin.withPollInterval
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit

class MigreringVerdikjedeTest : AbstractVerdikjedetest() {

    @Test
    fun `Skal ikke tillatte migrering av sak som ikke er BA OR OS`() {
        val callId = UUID.randomUUID().toString()
        MDC.put("callId", callId)

        Assertions.assertThatThrownBy {
            familieBaSakKlient().migrering(
                    lagTestScenarioForMigrering(
                            valg = "OR",
                            undervalg = "EU"
                    )!!.søker.ident!!
            )
        }.hasMessageContaining("Kan kun migrere ordinære saker")
        Assertions.assertThatThrownBy {
            familieBaSakKlient().migrering(
                    lagTestScenarioForMigrering(
                            valg = "OR",
                            undervalg = "IB"
                    )!!.søker.ident!!
            )
        }.hasMessageContaining("Kan kun migrere ordinære saker")
        Assertions.assertThatThrownBy {
            familieBaSakKlient().migrering(
                    lagTestScenarioForMigrering(
                            valg = "UT",
                            undervalg = "EF"
                    )!!.søker.ident!!
            )
        }.hasMessageContaining("Kan kun migrere ordinære saker")
        Assertions.assertThatThrownBy {
            familieBaSakKlient().migrering(
                    lagTestScenarioForMigrering(
                            valg = "UT",
                            undervalg = "EU"
                    )!!.søker.ident!!
            )
        }.hasMessageContaining("Kan kun migrere ordinære saker")
    }



    private fun lagTestScenarioForMigrering(valg: String? = "OR", undervalg: String? = "OS"): RestScenario? {
        val barn = mockServerKlient().lagScenario(
                RestScenario(
                        søker = RestScenarioPerson(
                                fødselsdato = LocalDate.now().minusYears(1).format(DateTimeFormatter.ISO_DATE),
                                fornavn = "Barn",
                                etternavn = "Barn"
                        ), barna = emptyList()
                )
        )

        val scenarioMorMedBarn = mockServerKlient().lagScenario(
                RestScenario(
                        søker = RestScenarioPerson(
                                fødselsdato = "1990-04-20",
                                fornavn = "Mor",
                                etternavn = "Søker",
                                infotrygdSaker = InfotrygdSøkResponse(
                                        bruker = listOf(
                                                lagInfotrygdSak(
                                                        nyttTilleggOrdinærSats.beløp.toDouble(),
                                                        barn.søker.ident!!,
                                                        valg,
                                                        undervalg
                                                )
                                        ), barn = emptyList()
                                )
                        ),
                        barna = listOf(
                                RestScenarioPerson(
                                        fødselsdato = LocalDate.now().minusYears(7).toString(),
                                        fornavn = "Barn",
                                        etternavn = "Barnesen",
                                        ident = barn.søker.ident,
                                )
                        )
                )
        )
        return scenarioMorMedBarn
    }

    protected fun erTaskOpprettetISak(taskStepType: String, callId: String) {
        try {
            await.atMost(60, TimeUnit.SECONDS)
                    .withPollInterval(Duration.ofSeconds(1))
                    .until {
                        sjekkOmTaskEksistererISak(taskStepType, callId)
                    }
        } catch (e: ConditionTimeoutException) {
            error("TaskStepType $taskStepType ikke opprettet for callId $callId")
        }
    }

    protected fun sjekkOmTaskEksistererISak(taskStepType: String, callId: String): Boolean {
        val tasker = familieBaSakKlient().hentTasker("callId", callId)
        try {
            Assertions.assertThat(tasker.body)
                    .hasSizeGreaterThan(0)
                    .extracting("taskStepType").contains(taskStepType)
        } catch (e: AssertionError) {
            return false
        }
        return true
    }
}
