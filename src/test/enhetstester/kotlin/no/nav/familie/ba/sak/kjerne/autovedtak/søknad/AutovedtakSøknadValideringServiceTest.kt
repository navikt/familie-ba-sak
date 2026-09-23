package no.nav.familie.ba.sak.kjerne.autovedtak.søknad

import io.mockk.mockk
import no.nav.familie.ba.sak.common.AutovedtakMåBehandlesManueltFeil
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagØkonomiSimuleringMottaker
import no.nav.familie.ba.sak.datagenerator.lagØkonomiSimuleringPostering
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.kontrakter.felles.simulering.PosteringType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class AutovedtakSøknadValideringServiceTest {
    private val vilkårsvurderingService = mockk<VilkårsvurderingService>()
    private val beregningService = mockk<BeregningService>()
    private val søknadGrunnlagService = mockk<SøknadGrunnlagService>()
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()

    private val autovedtakSøknadValideringService =
        AutovedtakSøknadValideringService(
            vilkårsvurderingService = vilkårsvurderingService,
            beregningService = beregningService,
            søknadGrunnlagService = søknadGrunnlagService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
        )

    private val behandling =
        lagBehandling(
            årsak = BehandlingÅrsak.AUTOMATISK_BEHANDLING_AV_SØKNAD,
            skalBehandlesAutomatisk = true,
            resultat = Behandlingsresultat.INNVILGET,
        )

    @Nested
    inner class ValiderAtSimuleringGirUtbetalingUtenFeilutbetaling {
        @Test
        fun `skal ikke kaste feil når simuleringen gir utbetaling uten feilutbetaling`() {
            // Arrange
            val simulering =
                listOf(lagØkonomiSimuleringMottaker(behandling = behandling, økonomiSimuleringPostering = listOf(lagØkonomiSimuleringPostering(beløp = 100))))

            // Act & Assert
            assertDoesNotThrow { autovedtakSøknadValideringService.validerAtSimuleringGirUtbetalingUtenFeilutbetaling(simulering) }
        }

        @Test
        fun `skal kaste feil når simuleringen gir feilutbetaling`() {
            // Arrange
            val simulering =
                listOf(
                    lagØkonomiSimuleringMottaker(
                        behandling = behandling,
                        økonomiSimuleringPostering = listOf(lagØkonomiSimuleringPostering(beløp = 100, posteringType = PosteringType.FEILUTBETALING)),
                    ),
                )

            // Act & Assert
            assertThrows<AutovedtakMåBehandlesManueltFeil> { autovedtakSøknadValideringService.validerAtSimuleringGirUtbetalingUtenFeilutbetaling(simulering) }
        }

        @Test
        fun `skal kaste feil når simuleringen ikke gir utbetaling`() {
            // Arrange
            val simuleringUtenUtbetaling =
                listOf(lagØkonomiSimuleringMottaker(behandling = behandling, økonomiSimuleringPostering = listOf(lagØkonomiSimuleringPostering(beløp = 0))))

            // Act & Assert
            assertThrows<AutovedtakMåBehandlesManueltFeil> { autovedtakSøknadValideringService.validerAtSimuleringGirUtbetalingUtenFeilutbetaling(simuleringUtenUtbetaling) }
        }
    }
}
