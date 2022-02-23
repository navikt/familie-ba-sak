package no.nav.familie.ba.sak.kjerne.brev

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ba.sak.integrasjoner.sanity.hentSanityBegrunnelser
import no.nav.familie.ba.sak.kjerne.brev.domene.BrevBegrunnelserTestConfig
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.hentGyldigeBegrunnelserForVedtaksperiodeMinimert
import no.nav.familie.kontrakter.felles.objectMapper
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestReporter
import java.io.File

@Disabled
class BrevBegrunnelseTest {
    @Test
    fun `Tester begrunnelser knyttet til brev`(testReporter: TestReporter) {
        val testmappe = File("./src/test/resources/brevbegrunnelserCaser")

        val sanityBegrunnelser = hentSanityBegrunnelser()

        val antallFeil = testmappe.list()?.fold(0) { acc, it ->
            val fil = File("$testmappe/$it")

            val brevBegrunnelserTestConfig =
                try {
                    objectMapper.readValue<BrevBegrunnelserTestConfig>(fil.readText())
                } catch (e: Exception) {
                    testReporter.publishEntry("Feil i fil: $it")
                    testReporter.publishEntry(e.message)
                    return@fold acc + 1
                }

            val begrunnelser = try {
                hentGyldigeBegrunnelserForVedtaksperiodeMinimert(
                    minimertVedtaksperiode = brevBegrunnelserTestConfig.hentMinimertVedtaksperiode(),
                    sanityBegrunnelser = sanityBegrunnelser,
                    minimertePersoner = brevBegrunnelserTestConfig.hentMinimertePersoner(),
                    minimertePersonresultater = brevBegrunnelserTestConfig.hentMinimertePersonResultater(),
                    aktørIderMedUtbetaling = brevBegrunnelserTestConfig.hentAktørIderMedUtbetaling(),
                    minimerteEndredeUtbetalingAndeler = brevBegrunnelserTestConfig.hentEndretUtbetalingAndeler(),
                    erFørsteVedtaksperiodePåFagsak = brevBegrunnelserTestConfig.erFørsteVedtaksperiodePåFagsak,
                    ytelserForSøkerForrigeMåned = brevBegrunnelserTestConfig.ytelserForSøkerForrigeMåned,
                    utvidetScenarioForEndringsperiode = brevBegrunnelserTestConfig.utvidetScenarioForEndringsperiode,
                )
            } catch (e: Exception) {
                testReporter.publishEntry(
                    "Feil i test: $it" +
                        "\nFeilmelding: ${e.message}" +
                        "\nFil: ${e.stackTrace.first()}" +
                        "\n-----------------------------------\n"
                )
                return@fold acc + 1
            }

            val feil = erLike(
                forventetOutput = brevBegrunnelserTestConfig.forventetOutput,
                output = begrunnelser
            )

            if (feil.isNotEmpty()) {
                testReporter.publishEntry(
                    it,
                    "${brevBegrunnelserTestConfig.beskrivelse}\n\n" +
                        feil.joinToString("\n\n") +
                        "\n-----------------------------------\n"
                )
                acc + 1
            } else {
                acc
            }
        }

        assert(antallFeil == 0)
    }

    private fun erLike(
        forventetOutput: List<VedtakBegrunnelseSpesifikasjon>,
        output: List<VedtakBegrunnelseSpesifikasjon>
    ): List<String> {

        val feil = mutableListOf<String>()

        if (forventetOutput.size != output.size) {
            feil.add(
                "Forventet antall begrunnelser var ${forventetOutput.size} begrunnelser, " +
                    "men fikk ${output.size}."
            )
        }
        forventetOutput.forEach { begrunnelse ->
            if (!output.contains(begrunnelse)) {
                feil.add(
                    "Forventet begrunnelse '$begrunnelse' var ikke i output."
                )
            }
        }

        output.forEach { begrunnelse ->
            if (!forventetOutput.contains(begrunnelse)) {
                feil.add(
                    "Begrunnelse '$begrunnelse' kom med i output, men var ikke med i forventet output."
                )
            }
        }

        if (feil.isNotEmpty()) {
            feil.add(
                "\nForventede begrunnelser: $forventetOutput" +
                    "\nOutput: $output"
            )
        }

        return feil
    }
}
