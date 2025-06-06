package no.nav.familie.ba.sak.kjerne.søknad

import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.søknad.SøknadMapper.Companion.tilBehandlingUnderkategori
import no.nav.familie.kontrakter.ba.søknad.v4.Søknadstype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SøknadMapperTest {
    @Nested
    inner class TilBehandlingUnderkategori {
        @Test
        fun `skal mappe Søknadstype ORDINÆR til BehandlingUnderkategori ORDINÆR`() {
            // Act
            val behandlingUnderkategori = Søknadstype.ORDINÆR.tilBehandlingUnderkategori()

            // Assert
            assertThat(behandlingUnderkategori)
                .isEqualTo(BehandlingUnderkategori.ORDINÆR)
        }

        @Test
        fun `skal mappe Søknadstype UTVIDET til BehandlingUnderkategori UTVIDET`() {
            // Act
            val behandlingUnderkategori = Søknadstype.UTVIDET.tilBehandlingUnderkategori()

            // Assert
            assertThat(behandlingUnderkategori)
                .isEqualTo(BehandlingUnderkategori.UTVIDET)
        }

        @Test
        fun `Søknadstype IKKE_SATT skal kaste feil`() {
            // Act & Assert
            val exception = assertThrows<IllegalArgumentException> { Søknadstype.IKKE_SATT.tilBehandlingUnderkategori() }
            assertThat(exception.message).isEqualTo("Søknadstype i Søknad må være satt for innsendte søknader: ${Søknadstype.IKKE_SATT}")
        }
    }
}
