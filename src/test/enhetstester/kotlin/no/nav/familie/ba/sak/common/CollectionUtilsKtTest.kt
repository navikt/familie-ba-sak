package no.nav.familie.ba.sak.common

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CollectionUtilsKtTest {
    @Nested
    inner class ContainsExactlyTest {
        @Test
        fun `skal returnere false om en tom liste ikke inneholder det ene ønskede elemente`() {
            // Arrange
            val collection = emptyList<String>()

            // Act
            val containsOnly = collection.containsExactly("A")

            // Assert
            assertThat(containsOnly).isFalse()
        }

        @Test
        fun `skal returnere true om en liste kun inneholder det ene ønskede elemente`() {
            // Arrange
            val collection = listOf("A")

            // Act
            val containsOnly = collection.containsExactly("A")

            // Assert
            assertThat(containsOnly).isTrue()
        }

        @Test
        fun `skal returnere true om en liste kun inneholder de ønskede ulike elemente`() {
            // Arrange
            val collection = listOf("A", "B")

            // Act
            val containsOnly = collection.containsExactly("A", "B")

            // Assert
            assertThat(containsOnly).isTrue()
        }

        @Test
        fun `skal returnere true om en liste kun inneholder de ønskede like elemente`() {
            // Arrange
            val collection = listOf("A", "A")

            // Act
            val containsOnly = collection.containsExactly("A", "A")

            // Assert
            assertThat(containsOnly).isTrue()
        }

        @Test
        fun `skal returnere false om en liste kun inneholder de ønskede ulike elemente i forskjellig rekkefølge`() {
            // Arrange
            val collection = listOf("A", "B")

            // Act
            val containsOnly = collection.containsExactly("B", "A")

            // Assert
            assertThat(containsOnly).isFalse()
        }

        @Test
        fun `skal returnere false om en liste med ett element ikke inneholder det ønskede elementet`() {
            // Arrange
            val collection = listOf("A")

            // Act
            val containsOnly = collection.containsExactly("B")

            // Assert
            assertThat(containsOnly).isFalse()
        }

        @Test
        fun `skal returnere false om en liste med ett element ikke inneholder de ønskede ulike elementene`() {
            // Arrange
            val collection = listOf("A")

            // Act
            val containsOnly = collection.containsExactly("A", "B")

            // Assert
            assertThat(containsOnly).isFalse()
        }

        @Test
        fun `skal returnere false om en liste med ett element ikke inneholder de ønskede like elementene`() {
            // Arrange
            val collection = listOf("A")

            // Act
            val containsOnly = collection.containsExactly("A", "A")

            // Assert
            assertThat(containsOnly).isFalse()
        }

        @Test
        fun `skal returnere false om et tomt set ikke inneholder det ene ønskede elemente`() {
            // Arrange
            val collection = emptySet<String>()

            // Act
            val containsOnly = collection.containsExactly("A")

            // Assert
            assertThat(containsOnly).isFalse()
        }

        @Test
        fun `skal returnere true om et set kun inneholder det ene ønskede elemente`() {
            // Arrange
            val collection = setOf("A")

            // Act
            val containsOnly = collection.containsExactly("A")

            // Assert
            assertThat(containsOnly).isTrue()
        }

        @Test
        fun `skal returnere true om et set kun inneholder de ønskede ulike elemente`() {
            // Arrange
            val collection = setOf("A", "B")

            // Act
            val containsOnly = collection.containsExactly("A", "B")

            // Assert
            assertThat(containsOnly).isTrue()
        }

        @Test
        fun `skal returnere false om et set kun inneholder de ønskede like elemente da et set kun inneholder unike elementer`() {
            // Arrange
            val collection = setOf("A", "A")

            // Act
            val containsOnly = collection.containsExactly("A", "A")

            // Assert
            assertThat(containsOnly).isFalse()
        }

        @Test
        fun `skal returnere false om et set kun inneholder de ønskede ulike elemente i forskjellig rekkefølge`() {
            // Arrange
            val collection = setOf("A", "B")

            // Act
            val containsOnly = collection.containsExactly("B", "A")

            // Assert
            assertThat(containsOnly).isFalse()
        }

        @Test
        fun `skal returnere false om et set med ett element ikke inneholder det ønskede elementet`() {
            // Arrange
            val collection = setOf("A")

            // Act
            val containsOnly = collection.containsExactly("B")

            // Assert
            assertThat(containsOnly).isFalse()
        }

        @Test
        fun `skal returnere false om et set med ett element ikke inneholder de ønskede ulike elementene`() {
            // Arrange
            val collection = setOf("A")

            // Act
            val containsOnly = collection.containsExactly("A", "B")

            // Assert
            assertThat(containsOnly).isFalse()
        }

        @Test
        fun `skal returnere false om et set med ett element ikke inneholder de ønskede like elementene`() {
            // Arrange
            val collection = setOf("A")

            // Act
            val containsOnly = collection.containsExactly("A", "A")

            // Assert
            assertThat(containsOnly).isFalse()
        }
    }
}
