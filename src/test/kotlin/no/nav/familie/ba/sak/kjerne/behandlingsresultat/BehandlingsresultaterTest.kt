package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.kontrakter.felles.objectMapper
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestReporter
import java.io.File


data class BehandlingsresultatPersonTestConfig(
        val personer: List<BehandlingsresultatPerson>,
        val beskrivelse: String,
        val forventetResultat: BehandlingResultat
)


@Nested
class BehandlingsresultaterTest {

    @Test
    fun `Skal sjekke at forventet resultat blir utledet for alle json testfiler`(testReporter: TestReporter) {
        val testmappe = File("./src/test/resources/behandlingsresultatPersoner")

        val antallFeil = testmappe.list()?.fold(0) { acc, it ->
            val fil = File("./src/test/resources/behandlingsresultatPersoner/$it")
            val behandlingsresultatPersonTestConfig =
                    objectMapper.readValue<BehandlingsresultatPersonTestConfig>(fil.readText())

            val ytelsePersonerMedResultat =
                    YtelsePersonUtils.utledYtelsePersonerMedResultat(behandlingsresultatPersonTestConfig.personer)
            val behandlingsresultat =
                    BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(ytelsePersonerMedResultat)

            if (behandlingsresultatPersonTestConfig.forventetResultat != behandlingsresultat) {
                testReporter.publishEntry(it,
                                          "${behandlingsresultatPersonTestConfig.beskrivelse}\nForventet ${behandlingsresultatPersonTestConfig.forventetResultat}, men fikk $behandlingsresultat.")
                acc + 1
            } else acc
        } ?: 0

        assert(antallFeil == 0)
    }
}