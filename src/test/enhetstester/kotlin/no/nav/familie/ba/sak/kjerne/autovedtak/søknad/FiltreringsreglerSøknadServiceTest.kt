package no.nav.familie.ba.sak.kjerne.autovedtak.søknad

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFiltreringsreglerFaktaSøknad
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.FILTRERINGSREGLER_SØKNAD
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.FiltreringsregelEvaluator
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.domene.FiltreringResultat
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.domene.FiltreringResultatRepository
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Evaluering
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.steg.FiltrerAutomatiskBehandlingData
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FiltreringsreglerSøknadServiceTest {
    private val filtreringsregelEvaluator = mockk<FiltreringsregelEvaluator>()
    private val filtreringResultatRepository = mockk<FiltreringResultatRepository>()
    private val faktaOppretter = mockk<FaktaOppretter>()
    private val metrikker = mockk<Metrikker>()

    private val filtreringsreglerSøknadService =
        FiltreringsreglerSøknadService(
            filtreringResultatRepository = filtreringResultatRepository,
            filtreringsregelEvaluator = filtreringsregelEvaluator,
            faktaOppretter = faktaOppretter,
            metrikker = metrikker,
        )

    @BeforeEach
    fun setup() {
        every { metrikker.oppdaterMetrikker(any()) } just Runs
        every { filtreringResultatRepository.saveAll(any<List<FiltreringResultat>>()) } answers { firstArg() }
    }

    @Test
    fun `skal evaluere filtreringsreglene med fakta fra FaktaOppretter og lagre resultatene`() {
        // Arrange
        val behandling = lagBehandling()
        val data = FiltrerAutomatiskBehandlingData(randomFnr(), listOf(randomFnr()))
        val fakta = lagFiltreringsreglerFaktaSøknad()
        val evaluering =
            Evaluering(
                resultat = Resultat.OPPFYLT,
                evalueringÅrsaker = emptyList(),
                begrunnelse = "",
                identifikator = FILTRERINGSREGLER_SØKNAD.first().identifikator.name,
            )
        val lagredeResultaterSlot = slot<List<FiltreringResultat>>()

        every { faktaOppretter.opprettFakta(data, behandling) } returns fakta
        every { filtreringsregelEvaluator.evaluerFiltreringsregler(FILTRERINGSREGLER_SØKNAD, fakta) } returns listOf(evaluering)
        every { filtreringResultatRepository.saveAll(capture(lagredeResultaterSlot)) } answers { firstArg() }

        // Act
        val resultater =
            filtreringsreglerSøknadService.kjørFiltreringsregler(
                filtrerAutomatiskBehandlingData = data,
                behandling = behandling,
            )

        // Assert
        assertThat(resultater).hasSize(1)
        assertThat(lagredeResultaterSlot.captured).hasSize(1)
        assertThat(lagredeResultaterSlot.captured.single().behandlingId).isEqualTo(behandling.id)
    }
}
