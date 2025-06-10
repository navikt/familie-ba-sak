package no.nav.familie.ba.sak.common.validering

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class OrganisasjonsnummerValidatorTest {
    private val organisasjonsnummerValidator = OrganisasjonsnummerValidator()

    @Nested
    inner class IsValid {
        @ParameterizedTest(name = "={0}")
        @ValueSource(strings = ["889640782", "310028142"])
        fun `Gyldige organisasjonsnummer for både prod og test skal returnere true`(organisasjonsnummer: String) {
            // Act
            val isValid = organisasjonsnummerValidator.isValid(organisasjonsnummer, null)

            // Assert
            assertThat(isValid).isTrue()
        }

        @Test
        fun `Ugyldig organisasjonsnummer pga lengde skal returnere false`() {
            // Act
            val isValid = organisasjonsnummerValidator.isValid("12345678", null)

            // Assert
            assertThat(isValid).isFalse()
        }

        @Test
        fun `Ugyldig organisasjonsnummer skal returnere false`() {
            // Act
            val isValid = organisasjonsnummerValidator.isValid("123456789", null)

            // Assert
            assertThat(isValid).isFalse()
        }
    }
}
