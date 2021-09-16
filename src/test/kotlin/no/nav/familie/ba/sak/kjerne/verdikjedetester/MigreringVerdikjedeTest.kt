package no.nav.familie.ba.sak.kjerne.verdikjedetester

import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.beregning.SatsService.nyttTilleggOrdinærSats
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.logg.LoggType
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenarioPerson
import no.nav.familie.kontrakter.ba.infotrygd.InfotrygdSøkResponse
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.getDataOrThrow
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class MigreringVerdikjedeTest(
        @Autowired private val behandlingService: BehandlingService,
        @Autowired private val fagsakService: FagsakService,
        @Autowired private val stegService: StegService,
        @Autowired private val vedtakService: VedtakService,
        @Autowired private val vedtaksperiodeService: VedtaksperiodeService
) : AbstractVerdikjedetest() {

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

    @Test
    fun `Skal migrere en bruker som har sak BA OR OS i infotrygd barnetrygd tjenesten`() {
        val callId = UUID.randomUUID().toString()
        MDC.put("callId", callId)

        val scenarioMorMedBarn = lagTestScenarioForMigrering()

        println("Skal migrerer ${scenarioMorMedBarn.søker.ident!!}")

        val migreringRessurs = familieBaSakKlient().migrering(scenarioMorMedBarn.søker.ident)
        println("Kall mot migrering returnerte $migreringRessurs")

        assertEquals("Migrering påbegynt", migreringRessurs.melding)

        val behandlingEtterVurdering = behandlingService.hent(behandlingId = migreringRessurs.data!!.behandlingId)
        assertEquals(BehandlingÅrsak.MIGRERING, behandlingEtterVurdering.opprettetÅrsak)
        assertEquals(BehandlingStatus.IVERKSETTER_VEDTAK, behandlingEtterVurdering.status)

        val utbetalingsperioder =
                vedtaksperiodeService.hentUtbetalingsperioder(behandling =behandlingEtterVurdering)
        assertEquals(2, utbetalingsperioder.size)


        val gjeldendeUtbetalingsperiode = utbetalingsperioder.find {
            it.periodeFom.toYearMonth() >= SatsService.tilleggOrdinærSatsNesteMånedTilTester.gyldigFom.toYearMonth() &&
            it.periodeFom.toYearMonth() <= SatsService.tilleggOrdinærSatsNesteMånedTilTester.gyldigTom.toYearMonth()
        }!!
        assertUtbetalingsperiode(gjeldendeUtbetalingsperiode, 1, SatsService.tilleggOrdinærSatsNesteMånedTilTester.beløp)

        val ferdigstiltBehandling = håndterIverksettingAvBehandling(
                behandlingEtterVurdering = behandlingEtterVurdering,
                søkerFnr = scenarioMorMedBarn.søker.ident,
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                stegService = stegService
        )
        assertEquals(BehandlingStatus.AVSLUTTET, ferdigstiltBehandling.status)

        val behandlingslogg = familieBaSakKlient().hentBehandlingslogg(migreringRessurs.data!!.behandlingId)
        println("Validerer sakslogg etter migrering $behandlingslogg")
        assertEquals(Ressurs.Status.SUKSESS, behandlingslogg.status)
        assertTrue(behandlingslogg.getDataOrThrow()
                           .filter { it.type == LoggType.BEHANDLING_OPPRETTET && it.tittel == "Migrering fra infotrygd opprettet" }.size == 1)
        assertTrue(behandlingslogg.getDataOrThrow().none { it.type == LoggType.DISTRIBUERE_BREV })
        assertTrue(behandlingslogg.getDataOrThrow().filter { it.type == LoggType.FERDIGSTILLE_BEHANDLING }.size == 1)
    }


    private fun lagTestScenarioForMigrering(valg: String? = "OR", undervalg: String? = "OS"): RestScenario {
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
}
