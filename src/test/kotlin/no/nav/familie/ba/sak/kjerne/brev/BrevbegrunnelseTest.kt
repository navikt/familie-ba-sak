package no.nav.familie.ba.sak.kjerne.brev

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ba.sak.common.Utils.formaterBeløp
import no.nav.familie.ba.sak.kjerne.brev.domene.BegrunnelseDataTestConfig
import no.nav.familie.ba.sak.kjerne.brev.domene.BrevGrunnlag
import no.nav.familie.ba.sak.kjerne.brev.domene.BrevPeriodeGrunnlag
import no.nav.familie.ba.sak.kjerne.brev.domene.BrevPeriodeOutput
import no.nav.familie.ba.sak.kjerne.brev.domene.BrevPeriodeTestConfig
import no.nav.familie.ba.sak.kjerne.brev.domene.FritekstBegrunnelseTestConfig
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriode
import no.nav.familie.kontrakter.felles.objectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestReporter
import java.io.File

class BrevbegrunnelseTest {

    @Test
    fun test(testReporter: TestReporter) {
        val testmappe = File("./src/test/resources/brevbegrunnelseCaser")

        val sanityBegrunnelser = hentSanityBegrunnelser()

        val antallFeil = testmappe.list()?.fold(0) { acc, it ->
            val fil = File("./src/test/resources/brevbegrunnelseCaser/$it")

            val behandlingsresultatPersonTestConfig =
                try {
                    objectMapper.readValue<BrevPeriodeTestConfig>(fil.readText())
                } catch (e: Exception) {
                    testReporter.publishEntry("Feil i fil: $it")
                    testReporter.publishEntry(e.message)
                    return@fold acc + 1
                }

            val brevPeriodeGrunnlag =
                BrevPeriodeGrunnlag(
                    fom = behandlingsresultatPersonTestConfig.fom,
                    tom = behandlingsresultatPersonTestConfig.tom,
                    type = behandlingsresultatPersonTestConfig.vedtaksperiodetype,
                    begrunnelser = behandlingsresultatPersonTestConfig
                        .begrunnelser.map { it.tilBrevBegrunnelseGrunnlag(sanityBegrunnelser) },
                    fritekster = behandlingsresultatPersonTestConfig.fritekster,
                    minimerteUtbetalingsperiodeDetaljer = behandlingsresultatPersonTestConfig
                        .personerPåBehandling
                        .flatMap { it.tilUtbetalingsperiodeDetaljer() }
                )

            val brevGrunnlag = BrevGrunnlag(
                personerPåBehandling = behandlingsresultatPersonTestConfig.personerPåBehandling.map { it.tilMinimertPerson() },
                minimerteEndredeUtbetalingAndeler = behandlingsresultatPersonTestConfig.personerPåBehandling.flatMap { it.tilMinimerteEndredeUtbetalingAndeler() },
                minimertePersonResultater = behandlingsresultatPersonTestConfig.personerPåBehandling.map { it.tilMinimertePersonResultater() }
            )

            val brevperiode: BrevPeriode? =
                brevPeriodeGrunnlag.tilBrevPeriode(
                    brevGrunnlag = brevGrunnlag,
                    utvidetScenarioForEndringsperiode = behandlingsresultatPersonTestConfig.utvidetScenarioForEndringsperiode,
                    uregistrerteBarn = behandlingsresultatPersonTestConfig.uregistrerteBarn,
                    erFørsteVedtaksperiodePåFagsak = behandlingsresultatPersonTestConfig.erFørsteVedtaksperiodePåFagsak,
                    brevMålform = behandlingsresultatPersonTestConfig.brevMålform,
                )

            val feil = erLike(
                forventetOutput = behandlingsresultatPersonTestConfig.forventetOutput,
                output = brevperiode
            )

            if (feil.isNotEmpty()) {
                testReporter.publishEntry(
                    it,
                    "${behandlingsresultatPersonTestConfig.beskrivelse}\n\n" +
                        feil.joinToString("\n\n") +
                        "\n-----------------------------------"
                )
                acc + 1
            } else {
                acc
            }
        }

        assert(antallFeil == 0)
    }

    private fun erLike(
        forventetOutput: BrevPeriodeOutput?,
        output: BrevPeriode?
    ): List<String> {

        val feil = mutableListOf<String>()

        fun validerFelt(forventet: String?, faktisk: String?, variabelNavn: String) {
            if (forventet != faktisk) {
                feil.add(
                    "Forventet $variabelNavn var: '$forventet', men fikk '$faktisk'"
                )
            }
        }

        if (forventetOutput == null || output == null) {
            if (forventetOutput != null)
                feil.add("Output er null, men forventet output er $forventetOutput.")
            if (output != null)
                feil.add("Forventet output er null, men output er $output.")
        } else {
            validerFelt(forventetOutput.fom, output.fom?.single(), "fom")
            validerFelt(forventetOutput.tom, output.tom?.single(), "tom")
            validerFelt(forventetOutput.type, output.type?.single(), "type")
            validerFelt(forventetOutput.barnasFodselsdager, output.barnasFodselsdager?.single(), "barnasFodselsdager")
            validerFelt(forventetOutput.antallBarn, output.antallBarn?.single(), "antallBarn")
            validerFelt(
                if (forventetOutput.belop != null)
                    formaterBeløp(forventetOutput.belop)
                else null,
                output.belop?.single(), "belop"
            )

            val forventedeBegrunnelser = forventetOutput.begrunnelser.map {
                when (it) {
                    is BegrunnelseDataTestConfig -> it.tilBegrunnelseData()
                    is FritekstBegrunnelseTestConfig -> it.fritekst
                    else -> throw IllegalArgumentException("Ugyldig testconfig")
                }
            }

            if (forventedeBegrunnelser.size != output.begrunnelser.size) {
                feil.add(
                    "Forventet antall begrunnelser var ${forventedeBegrunnelser.size} begrunnelser, " +
                        "men fikk ${output.begrunnelser.size}." +
                        "\nForventede begrunnelser: $forventedeBegrunnelser" +
                        "\nOutput: ${output.begrunnelser}"
                )
            } else {
                forventedeBegrunnelser.forEachIndexed { index, _ ->
                    if (forventedeBegrunnelser[index] != output.begrunnelser[index]) {
                        feil.add(
                            "Forventet begrunnelse nr. ${index + 1} var: '${forventedeBegrunnelser[index]}', " +
                                "men fikk '${output.begrunnelser[index]}'"
                        )
                    }
                }
            }
        }
        return feil
    }
}

