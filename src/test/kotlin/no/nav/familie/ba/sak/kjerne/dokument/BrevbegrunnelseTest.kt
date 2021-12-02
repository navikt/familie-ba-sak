package no.nav.familie.ba.sak.kjerne.dokument

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ba.sak.ekstern.restDomene.RestPerson
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.UregistrertBarnEnkel
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.brevperioder.BrevPeriode
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BegrunnelseData
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BegrunnelsePerson
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.RestVedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.UtbetalingsperiodeDetalj
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.UtbetalingsperiodeDetaljEnkel
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.kontrakter.felles.objectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestReporter
import java.io.File
import java.time.LocalDate

data class BrevBegrunnelseTestConfig(
    val beskrivelse: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val vedtaksperiodetype: Vedtaksperiodetype,
    val utbetalingsperiodeDetaljer: List<UtbetalingsperiodeDetaljEnkel>,
    val standardbegrunnelser: List<RestVedtaksbegrunnelse>,
    val fritekster: List<String>,
    val begrunnelsepersoner: List<BegrunnelsePerson>,
    val målform: Målform,
    val uregistrerteBarn: List<UregistrertBarnEnkel>,
    val forventetOutput: BrevPeriodeTestConfig
)

data class BrevPeriodeTestConfig(
    val fom: String,
    val tom: String,
    val belop: String,
    val antallBarn: String,
    val barnasFodselsdager: String,
    val begrunnelser: List<BegrunnelseData>,
    val type: String,
)

private fun BegrunnelsePerson.tilRestPersonTilTester() = RestPerson(
    personIdent = this.personIdent,
    fødselsdato = this.fødselsdato,
    type = this.type,
    navn = "Mock Mockersen",
    kjønn = Kjønn.KVINNE,
    målform = Målform.NB
)

private fun UtbetalingsperiodeDetaljEnkel.tilUtbetalingsperiodeDetalj(restPerson: RestPerson) =
    UtbetalingsperiodeDetalj(
        person = restPerson,
        utbetaltPerMnd = this.utbetaltPerMnd,
        prosent = this.prosent,
        erPåvirketAvEndring = this.erPåvirketAvEndring,
        ytelseType = this.ytelseType
    )

class BrevbegrunnelseTest {

    @Test
    fun test(testReporter: TestReporter) {
        val testmappe = File("./src/test/resources/brevbegrunnelseCaser")

        val antallFeil = testmappe.list()?.fold(0) { acc, it ->
            val fil = File("./src/test/resources/brevbegrunnelseCaser/$it")
            val behandlingsresultatPersonTestConfig =
                objectMapper.readValue<BrevBegrunnelseTestConfig>(fil.readText())

            val restPersoner =
                behandlingsresultatPersonTestConfig.begrunnelsepersoner.map { it.tilRestPersonTilTester() }

            val utvidetVedtaksperiodeMedBegrunnelser = UtvidetVedtaksperiodeMedBegrunnelser(
                id = 1L,
                fom = behandlingsresultatPersonTestConfig.fom,
                tom = behandlingsresultatPersonTestConfig.tom,
                type = behandlingsresultatPersonTestConfig.vedtaksperiodetype,
                begrunnelser = behandlingsresultatPersonTestConfig.standardbegrunnelser,
                fritekster = behandlingsresultatPersonTestConfig.fritekster,
                gyldigeBegrunnelser = emptyList(),
                utbetalingsperiodeDetaljer = behandlingsresultatPersonTestConfig.utbetalingsperiodeDetaljer.map {
                    it.tilUtbetalingsperiodeDetalj(
                        restPersoner.find { restPerson -> restPerson.personIdent == it.personIdent }!!
                    )
                }
            )

            val brevperiode: BrevPeriode? =
                utvidetVedtaksperiodeMedBegrunnelser.tilBrevPeriode(
                    begrunnelsepersonerIBehandling = behandlingsresultatPersonTestConfig.begrunnelsepersoner,
                    målform = behandlingsresultatPersonTestConfig.målform,
                    uregistrerteBarn = behandlingsresultatPersonTestConfig.uregistrerteBarn,
                    utvidetScenario = UtvidetScenario.IKKE_UTVIDET_YTELSE
                )


            if (!erLike(
                    testReporter = testReporter,
                    forventetOutput = behandlingsresultatPersonTestConfig.forventetOutput,
                    output = brevperiode
                )
            ) {
                testReporter.publishEntry(
                    it,
                    "${behandlingsresultatPersonTestConfig.beskrivelse}\nForventet ${behandlingsresultatPersonTestConfig.forventetOutput}, men fikk ${brevperiode}."
                )
                acc + 1
            } else {
                0
            }
        }

        assert(antallFeil == 0)
    }

    private fun erLike(
        testReporter: TestReporter,
        forventetOutput: BrevPeriodeTestConfig?,
        output: BrevPeriode?
    ): Boolean {

        return when {
            forventetOutput == null && output == null -> true
            forventetOutput != null && output == null -> false
            forventetOutput == null && output != null -> false
            forventetOutput?.fom != output?.fom?.single() -> false
            forventetOutput?.tom != output?.tom?.single() -> false
            forventetOutput?.type != output?.type?.single() -> false
            forventetOutput?.barnasFodselsdager != output?.barnasFodselsdager?.single() -> false
            forventetOutput?.antallBarn != output?.antallBarn?.single() -> false
            forventetOutput?.belop != output?.belop?.single() -> false
            forventetOutput?.antallBarn != output?.antallBarn?.single() -> false
            forventetOutput?.begrunnelser?.any { output?.begrunnelser?.contains(it) == false } == true -> false
            else -> false
        }
    }
}
