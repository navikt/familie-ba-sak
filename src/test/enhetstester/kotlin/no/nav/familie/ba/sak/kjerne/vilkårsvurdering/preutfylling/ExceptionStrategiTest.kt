package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class ExceptionStrategiTest {
    @Nested
    inner class FraBehandlingÅrsak {
        @Test
        fun `skal returnere THROW for AUTOMATISK_BEHANDLING_AV_SØKNAD`() {
            // Act
            val exceptionStrategi = ExceptionStrategi.fraBehandlingÅrsak(BehandlingÅrsak.AUTOMATISK_BEHANDLING_AV_SØKNAD)

            // Assert
            assertThat(exceptionStrategi).isEqualTo(ExceptionStrategi.THROW)
        }

        @ParameterizedTest
        @EnumSource(value = BehandlingÅrsak::class, names = ["AUTOMATISK_BEHANDLING_AV_SØKNAD"], mode = EnumSource.Mode.EXCLUDE)
        fun `skal returnere SWALLOW for alle andre behandlingsårsaker`(årsak: BehandlingÅrsak) {
            // Act
            val exceptionStrategi = ExceptionStrategi.fraBehandlingÅrsak(årsak)

            // Assert
            assertThat(exceptionStrategi).isEqualTo(ExceptionStrategi.SWALLOW)
        }
    }
}
