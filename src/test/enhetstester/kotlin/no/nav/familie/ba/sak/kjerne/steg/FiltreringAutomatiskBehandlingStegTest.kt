package no.nav.familie.ba.sak.kjerne.steg

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFiltreringResultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.FiltreringsreglerFødselshendelseService
import no.nav.familie.ba.sak.kjerne.autovedtak.søknad.FiltreringsreglerSøknadService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.logg.Logg
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class FiltreringAutomatiskBehandlingStegTest {
    private val filtreringsreglerFødselshendelseService = mockk<FiltreringsreglerFødselshendelseService>()
    private val filtreringsreglerSøknadService = mockk<FiltreringsreglerSøknadService>()
    private val loggService = mockk<LoggService>()

    private val filtreringAutomatiskBehandlingSteg =
        FiltreringAutomatiskBehandlingSteg(
            filtreringsreglerFødselshendelseService = filtreringsreglerFødselshendelseService,
            filtreringsreglerSøknadService = filtreringsreglerSøknadService,
            loggService = loggService,
        )

    private val data = FiltrerAutomatiskBehandlingData(søkersIdent = "12345678910", barnasIdenter = listOf("10987654321"))

    @Nested
    inner class Fødselshendelse {
        @Test
        fun `skal logge i historikken og gå videre til vilkårsvurdering når alle filtreringsreglene er oppfylt`() {
            // Arrange
            val behandling = lagBehandling(årsak = BehandlingÅrsak.FØDSELSHENDELSE, skalBehandlesAutomatisk = true)
            val filtreringResultater = listOf(lagFiltreringResultat(behandlingId = behandling.id, resultat = Resultat.OPPFYLT))

            every { filtreringsreglerFødselshendelseService.kjørFiltreringsregler(data, behandling) } returns filtreringResultater
            every { loggService.opprettFiltreringsreglerLogg(behandling, filtreringResultater) } returns mockk<Logg>()

            // Act
            val nesteSteg = filtreringAutomatiskBehandlingSteg.utførStegOgAngiNeste(behandling, data)

            // Assert
            assertThat(nesteSteg).isEqualTo(StegType.VILKÅRSVURDERING)
            verify(exactly = 1) { loggService.opprettFiltreringsreglerLogg(behandling, filtreringResultater) }
            verify(exactly = 0) { filtreringsreglerSøknadService.kjørFiltreringsregler(any(), any()) }
        }

        @Test
        fun `skal logge i historikken og henlegge når en filtreringsregel ikke er oppfylt`() {
            // Arrange
            val behandling = lagBehandling(årsak = BehandlingÅrsak.FØDSELSHENDELSE, skalBehandlesAutomatisk = true)
            val filtreringResultater =
                listOf(lagFiltreringResultat(behandlingId = behandling.id, resultat = Resultat.IKKE_OPPFYLT, begrunnelse = "Det er registrert dødsdato på mor."))

            every { filtreringsreglerFødselshendelseService.kjørFiltreringsregler(data, behandling) } returns filtreringResultater
            every { loggService.opprettFiltreringsreglerLogg(behandling, filtreringResultater) } returns mockk<Logg>()

            // Act
            val nesteSteg = filtreringAutomatiskBehandlingSteg.utførStegOgAngiNeste(behandling, data)

            // Assert
            assertThat(nesteSteg).isEqualTo(StegType.HENLEGG_BEHANDLING)
            verify(exactly = 1) { loggService.opprettFiltreringsreglerLogg(behandling, filtreringResultater) }
            verify(exactly = 0) { filtreringsreglerSøknadService.kjørFiltreringsregler(any(), any()) }
        }
    }

    @Nested
    inner class AutomatiskBehandlingAvSøknad {
        @Test
        fun `skal logge i historikken og gå videre til vilkårsvurdering når alle filtreringsreglene er oppfylt`() {
            // Arrange
            val behandling = lagBehandling(årsak = BehandlingÅrsak.AUTOMATISK_BEHANDLING_AV_SØKNAD, skalBehandlesAutomatisk = true)
            val filtreringResultater = listOf(lagFiltreringResultat(behandlingId = behandling.id, resultat = Resultat.OPPFYLT))

            every { filtreringsreglerSøknadService.kjørFiltreringsregler(data, behandling) } returns filtreringResultater
            every { loggService.opprettFiltreringsreglerLogg(behandling, filtreringResultater) } returns mockk<Logg>()

            // Act
            val nesteSteg = filtreringAutomatiskBehandlingSteg.utførStegOgAngiNeste(behandling, data)

            // Assert
            assertThat(nesteSteg).isEqualTo(StegType.VILKÅRSVURDERING)
            verify(exactly = 1) { loggService.opprettFiltreringsreglerLogg(behandling, filtreringResultater) }
            verify(exactly = 0) { filtreringsreglerFødselshendelseService.kjørFiltreringsregler(any(), any()) }
        }

        @Test
        fun `skal logge i historikken og henlegge når en filtreringsregel ikke er oppfylt`() {
            // Arrange
            val behandling = lagBehandling(årsak = BehandlingÅrsak.AUTOMATISK_BEHANDLING_AV_SØKNAD, skalBehandlesAutomatisk = true)
            val filtreringResultater =
                listOf(lagFiltreringResultat(behandlingId = behandling.id, resultat = Resultat.IKKE_OPPFYLT, begrunnelse = "Det er registrert dødsdato på mor."))

            every { filtreringsreglerSøknadService.kjørFiltreringsregler(data, behandling) } returns filtreringResultater
            every { loggService.opprettFiltreringsreglerLogg(behandling, filtreringResultater) } returns mockk<Logg>()

            // Act
            val nesteSteg = filtreringAutomatiskBehandlingSteg.utførStegOgAngiNeste(behandling, data)

            // Assert
            assertThat(nesteSteg).isEqualTo(StegType.HENLEGG_BEHANDLING)
            verify(exactly = 1) { loggService.opprettFiltreringsreglerLogg(behandling, filtreringResultater) }
            verify(exactly = 0) { filtreringsreglerFødselshendelseService.kjørFiltreringsregler(any(), any()) }
        }
    }

    @Nested
    inner class AnnenBehandlingsårsak {
        @Test
        fun `skal kaste feil og ikke logge når behandlingsårsaken ikke skal filtreres`() {
            // Arrange
            val behandling = lagBehandling(årsak = BehandlingÅrsak.SØKNAD)

            // Act & Assert
            assertThrows<Feil> { filtreringAutomatiskBehandlingSteg.utførStegOgAngiNeste(behandling, data) }
            verify(exactly = 0) { loggService.opprettFiltreringsreglerLogg(any(), any()) }
        }
    }
}
