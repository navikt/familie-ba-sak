package no.nav.familie.ba.sak.kjerne.dokument

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ba.sak.common.Utils.formaterBeløp
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

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
    defaultImpl = BegrunnelseDataTestConfig::class
)
@JsonSubTypes(value = [JsonSubTypes.Type(value = FritekstBegrunnelseTestConfig::class, name = "fritekst")])
interface TestBegrunnelse

data class FritekstBegrunnelseTestConfig(val fritekst: String) : TestBegrunnelse

data class BegrunnelseDataTestConfig(
    val gjelderSoker: Boolean,
    val barnasFodselsdatoer: String,
    val antallBarn: Int,
    val maanedOgAarBegrunnelsenGjelderFor: String?,
    val maalform: String,
    val apiNavn: String,
    val belop: Int,
) : TestBegrunnelse

data class BrevPeriodeTestConfig(
    val fom: String,
    val tom: String,
    val belop: Int,
    val antallBarn: String,
    val barnasFodselsdager: String,
    val begrunnelser: List<TestBegrunnelse>,
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

fun BegrunnelseDataTestConfig.tilBegrunnelseData() = BegrunnelseData(
    belop = formaterBeløp(this.belop),
    gjelderSoker = this.gjelderSoker,
    barnasFodselsdatoer = this.barnasFodselsdatoer,
    antallBarn = this.antallBarn,
    maanedOgAarBegrunnelsenGjelderFor = this.maanedOgAarBegrunnelsenGjelderFor,
    maalform = this.maalform,
    apiNavn = this.apiNavn,
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

            val feil = erLike(
                forventetOutput = behandlingsresultatPersonTestConfig.forventetOutput,
                output = brevperiode
            )

            if (feil.isNotEmpty()) {
                testReporter.publishEntry(
                    it,
                    "${behandlingsresultatPersonTestConfig.beskrivelse}\n" +
                        feil.joinToString("\n")
                )
                acc + 1
            } else {
                acc
            }
        }

        assert(antallFeil == 0)
    }

    private fun erLike(
        forventetOutput: BrevPeriodeTestConfig?,
        output: BrevPeriode?
    ): List<String> {

        val feil = mutableListOf<String>()

        fun validerFelt(forventet: String, faktisk: String?, variabelNavn: String) {
            if (forventet != faktisk) {
                feil.add(
                    "Forventet $variabelNavn var: '$forventet', men fikk '$faktisk'"
                )
            }
        }

        if (forventetOutput == null || output == null) {
            if (forventetOutput != null && output == null)
                feil.add("Output er null, men forventet output er $forventetOutput.")
            if (forventetOutput == null && output != null)
                feil.add("Forventet output er null, men output er $output.")
        } else {
            validerFelt(forventetOutput.fom, output.fom?.single(), "fom")
            validerFelt(forventetOutput.tom, output.tom?.single(), "tom")
            validerFelt(forventetOutput.type, output.type?.single(), "type")
            validerFelt(forventetOutput.barnasFodselsdager, output.barnasFodselsdager?.single(), "barnasFodselsdager")
            validerFelt(forventetOutput.antallBarn, output.antallBarn?.single(), "antallBarn")
            validerFelt(formaterBeløp(forventetOutput.belop), output.belop?.single(), "belop")

            val forventedeBegrunnelser = forventetOutput.begrunnelser.map {
                when (it) {
                    is BegrunnelseDataTestConfig -> it.tilBegrunnelseData()
                    is FritekstBegrunnelseTestConfig -> it.fritekst
                    else -> throw IllegalArgumentException("Ugyldig testconfig")
                }
            }

            forventedeBegrunnelser.filter { !output.begrunnelser.contains(it) }.forEach {
                feil.add(
                    "Fant ingen begrunnelser i output-begrunnelsene som matcher forventet begrunnelse $it"
                )
            }
            output.begrunnelser.filter { !forventedeBegrunnelser.contains(it) }.forEach {
                feil.add(
                    "Fant ingen begrunnelser i de forventede begrunnelser som matcher begrunnelse $it"
                )
            }
        }

        return feil
    }
}
