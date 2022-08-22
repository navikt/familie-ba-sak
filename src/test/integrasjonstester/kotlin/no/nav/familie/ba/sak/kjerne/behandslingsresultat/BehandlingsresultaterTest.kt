package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.kontrakter.felles.objectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestReporter
import java.io.File
import java.time.YearMonth

data class BehandlingsresultatPersonTestConfig(
    val personer: List<BehandlingsresultatPerson>,
    val uregistrerteBarn: List<MinimertUregistrertBarn> = emptyList(),
    val beskrivelse: String,
    val forventetResultat: Behandlingsresultat,
    val inneværendeMåned: String,
)

class BehandlingsresultaterTest {
    private val personidentService = mockk<PersonidentService>()

    @BeforeEach
    fun init() {
        every { personidentService.hentAktør(any()) } answers { randomAktør() }
    }

    @Test
    fun `Skal sjekke at forventet resultat blir utledet for alle json testfiler`(testReporter: TestReporter) {
        val testmappe = File("./src/test/resources/behandlingsresultatPersoner")

        val antallFeil = testmappe.list()?.fold(0) { acc, it ->
            val fil = File("./src/test/resources/behandlingsresultatPersoner/$it")
            val behandlingsresultatPersonTestConfig =
                objectMapper.readValue<BehandlingsresultatPersonTestConfig>(fil.readText())

            val ytelsePersonerMedResultat =
                YtelsePersonUtils.utledYtelsePersonerMedResultat(
                    behandlingsresultatPersoner = behandlingsresultatPersonTestConfig.personer,
                    uregistrerteBarn = behandlingsresultatPersonTestConfig.uregistrerteBarn,
                    inneværendeMåned = YearMonth.parse(behandlingsresultatPersonTestConfig.inneværendeMåned),
                )

            val behandlingsresultat =
                BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(ytelsePersonerMedResultat)

            if (behandlingsresultatPersonTestConfig.forventetResultat != behandlingsresultat) {
                testReporter.publishEntry(
                    it,
                    "${behandlingsresultatPersonTestConfig.beskrivelse}\nForventet ${behandlingsresultatPersonTestConfig.forventetResultat}, men fikk $behandlingsresultat."
                )
                acc + 1
            } else {
                acc
            }
        } ?: 0

        assert(antallFeil == 0)
    }
}
