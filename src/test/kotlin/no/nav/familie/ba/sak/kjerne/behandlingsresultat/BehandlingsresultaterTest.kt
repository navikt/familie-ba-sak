package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.kontrakter.felles.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

data class BehandlingsresultatPersonTestConfig(
        val personer: List<BehandlingsresultatPerson>,
        val forventetResultat: BehandlingResultat
)


class BehandlingsresultaterTest {

    @Test
    fun `Skal sjekke at forventet resultat blir utledet for alle json testfiler`() {

        val file = File("./src/test/resources/behandlingsresultatPersoner")

        file.list()?.forEach {
            val fil = File("./src/test/resources/behandlingsresultatPersoner/$it")
            val behandlingsresultatPersonTestConfig =
                    objectMapper.readValue<BehandlingsresultatPersonTestConfig>(fil.readText())

            val ytelsePersonerMedResultat = YtelsePersonUtils.utledYtelsePersonerMedResultat(behandlingsresultatPersonTestConfig.personer)
            val behandlingsresultat = BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(ytelsePersonerMedResultat)
            assertEquals(behandlingsresultatPersonTestConfig.forventetResultat, behandlingsresultat)
        }

    }

}