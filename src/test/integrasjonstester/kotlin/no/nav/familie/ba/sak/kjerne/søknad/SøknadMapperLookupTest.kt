package no.nav.familie.ba.sak.kjerne.søknad

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.kontrakter.felles.søknad.MissingVersionException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired

class SøknadMapperLookupTest(
    @Autowired private val søknadMapperLookup: SøknadMapper.Lookup,
) : AbstractSpringIntegrationTest() {
    @Nested
    inner class HentSøknadMapperForVersjon {
        @ParameterizedTest
        @ValueSource(ints = [9, 10])
        fun `skal hente alle tilgjengelige mappere`(versjon: Int) {
            // Act
            val søknadMapper = søknadMapperLookup.hentSøknadMapperForVersjon(versjon)

            // Assert
            assertThat(søknadMapper).isNotNull
        }

        @Test
        fun `skal hente SøknadMapperV9 dersom versjon er 9`() {
            // Arrange
            val versjon = 9

            // Act
            val søknadMapper = søknadMapperLookup.hentSøknadMapperForVersjon(versjon)

            // Assert
            assertThat(søknadMapper).isInstanceOf(SøknadMapperV9::class.java)
        }

        @Test
        fun `skal hente SøknadMapperV10 dersom versjon er 10`() {
            // Arrange
            val versjon = 10

            // Act
            val søknadMapper = søknadMapperLookup.hentSøknadMapperForVersjon(versjon)

            // Assert
            assertThat(søknadMapper).isInstanceOf(SøknadMapperV10::class.java)
        }

        @Test
        fun `skal kaste MissingVersionException dersom dersom mapper for søknadversjon ikke finnes`() {
            // Arrange
            val versjon = 11

            // Act & Assert
            val exception = assertThrows<MissingVersionException> { søknadMapperLookup.hentSøknadMapperForVersjon(versjon) }
            assertThat(exception.message).isEqualTo("Mangler SøknadMapper for versjon 11")
        }
    }
}
