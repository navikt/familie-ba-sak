package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling.PreutfyllVilkårService.Companion.IKKE_PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling.PreutfyllVilkårService.Companion.PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class DelvilkårTest {
    @Nested
    inner class TilBegrunnelse {
        @Test
        fun `skal legge til preutfylt overskrift for oppfylt delvilkår`() {
            // Arrange
            val begrunnelse = "- Begrunnelse"

            // Act
            val resultat = OppfyltDelvilkår(begrunnelse = begrunnelse).tilBegrunnelse()

            // Assert
            assertThat(resultat).isEqualTo(PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT + begrunnelse)
        }

        @Test
        fun `skal legge til preutfylt overskrift for ikke oppfylt delvilkår`() {
            // Arrange
            val begrunnelse = "- Begrunnelse"

            // Act
            val resultat = IkkeOppfyltDelvilkår(begrunnelse = begrunnelse).tilBegrunnelse()

            // Assert
            assertThat(resultat).isEqualTo(PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT + begrunnelse)
        }

        @Test
        fun `skal legge til ikke preutfylt overskrift for ikke vurdert vilkår med begrunnelse`() {
            // Arrange
            val begrunnelse = "- Begrunnelse"

            // Act
            val resultat = IkkeVurdertVilkår(begrunnelse = begrunnelse).tilBegrunnelse()

            // Assert
            assertThat(resultat).isEqualTo(IKKE_PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT + begrunnelse)
        }

        @Test
        fun `skal bare legge til ikke preutfylt overskrift for ikke vurdert vilkår uten begrunnelse`() {
            // Arrange
            val delvilkår = IkkeVurdertVilkår()

            // Act
            val resultat = delvilkår.tilBegrunnelse()

            // Assert
            assertThat(resultat).isEqualTo(IKKE_PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT)
        }
    }

    @Nested
    inner class TilResultat {
        @Test
        fun `skal mappe oppfylt delvilkår til oppfylt resultat`() {
            // Arrange
            val delvilkår = OppfyltDelvilkår(begrunnelse = "- Begrunnelse")

            // Act
            val resultat = delvilkår.tilResultat()

            // Assert
            assertThat(resultat).isEqualTo(Resultat.OPPFYLT)
        }

        @Test
        fun `skal mappe ikke oppfylt delvilkår til ikke oppfylt resultat`() {
            // Arrange
            val delvilkår = IkkeOppfyltDelvilkår()

            // Act
            val resultat = delvilkår.tilResultat()

            // Assert
            assertThat(resultat).isEqualTo(Resultat.IKKE_OPPFYLT)
        }

        @Test
        fun `skal mappe ikke vurdert vilkår til ikke vurdert resultat`() {
            // Arrange
            val delvilkår = IkkeVurdertVilkår()

            // Act
            val resultat = delvilkår.tilResultat()

            // Assert
            assertThat(resultat).isEqualTo(Resultat.IKKE_VURDERT)
        }
    }
}
