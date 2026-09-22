package no.nav.familie.ba.sak.common

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class IdentUtilsKtTest {
    @Nested
    inner class ErDnummerTest {
        @Test
        fun `skal returnere false om første siffer er 0`() {
            // Arrange
            val personIdent = "01010100000"

            // Act
            val erDnummer = erDnummer(personIdent)

            // Assert
            assertThat(erDnummer).isFalse()
        }

        @Test
        fun `skal returnere false om første siffer er 3`() {
            // Arrange
            val personIdent = "31010100000"

            // Act
            val erDnummer = erDnummer(personIdent)

            // Assert
            assertThat(erDnummer).isFalse()
        }

        @Test
        fun `skal returnere true om første siffer er 4`() {
            // Arrange
            val personIdent = "41010100000"

            // Act
            val erDnummer = erDnummer(personIdent)

            // Assert
            assertThat(erDnummer).isTrue()
        }

        @Test
        fun `skal returnere true om første siffer er 7`() {
            // Arrange
            val personIdent = "71010100000"

            // Act
            val erDnummer = erDnummer(personIdent)

            // Assert
            assertThat(erDnummer).isTrue()
        }
    }

    @Nested
    inner class ErFDatnummerTest {
        @Test
        fun `skal returnere true om de fem siste sifrene er 00000`() {
            // Arrange
            val personIdent = "01010100000"

            // Act
            val erFDatnummer = erFDatnummer(personIdent)

            // Assert
            assertThat(erFDatnummer).isTrue()
        }

        @Test
        fun `skal returnere false om de fem siste sifrene ikke er 00000`() {
            // Arrange
            val personIdent = "01010112345"

            // Act
            val erFDatnummer = erFDatnummer(personIdent)

            // Assert
            assertThat(erFDatnummer).isFalse()
        }

        @Test
        fun `skal returnere false om kun personnummeret er 0 men kontrollsifrene ikke er det`() {
            // Arrange
            val personIdent = "01010100001"

            // Act
            val erFDatnummer = erFDatnummer(personIdent)

            // Assert
            assertThat(erFDatnummer).isFalse()
        }
    }

    @Nested
    inner class ErBostNummerTest {
        @Test
        fun `skal returnere false om måned er en vanlig måned`() {
            // Arrange
            val personIdent = "01010100000"

            // Act
            val erBostNummer = erBostNummer(personIdent)

            // Assert
            assertThat(erBostNummer).isFalse()
        }

        @Test
        fun `skal returnere false om måned er 20`() {
            // Arrange
            val personIdent = "01200100000"

            // Act
            val erBostNummer = erBostNummer(personIdent)

            // Assert
            assertThat(erBostNummer).isFalse()
        }

        @Test
        fun `skal returnere true om måned er nedre grense 21`() {
            // Arrange
            val personIdent = "01210100000"

            // Act
            val erBostNummer = erBostNummer(personIdent)

            // Assert
            assertThat(erBostNummer).isTrue()
        }

        @Test
        fun `skal returnere true om måned er øvre grense 32`() {
            // Arrange
            val personIdent = "01320100000"

            // Act
            val erBostNummer = erBostNummer(personIdent)

            // Assert
            assertThat(erBostNummer).isTrue()
        }

        @Test
        fun `skal returnere false om måned er 33`() {
            // Arrange
            val personIdent = "01330100000"

            // Act
            val erBostNummer = erBostNummer(personIdent)

            // Assert
            assertThat(erBostNummer).isFalse()
        }
    }
}
