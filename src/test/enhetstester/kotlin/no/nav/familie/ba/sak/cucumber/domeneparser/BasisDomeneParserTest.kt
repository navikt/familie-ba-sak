package no.nav.familie.ba.sak.cucumber.domeneparser

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import java.time.LocalDate
import java.time.YearMonth

class BasisDomeneParserTest {
    @Nested
    inner class PåkrevdeFelt {
        @Test
        fun `skal parse utfylt verdi selv om andre felt er null`() {
            // Arrange
            val rad: Tabellrad = mapOf("BehandlingId" to "1_234", "Til dato" to null)

            // Act
            val behandlingId = parseLong(Domenebegrep.BEHANDLING_ID, rad)

            // Assert
            assertThat(behandlingId).isEqualTo(1234L)
        }

        @ParameterizedTest
        @NullAndEmptySource
        fun `skal avvise null og tom streng for påkrevd felt`(celleverdi: String?) {
            // Arrange
            val rad: Tabellrad = mapOf("BehandlingId" to celleverdi)

            // Act & Assert
            assertThatThrownBy { parseLong(Domenebegrep.BEHANDLING_ID, rad) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Fant ingen verdi for BehandlingId")
        }

        @Test
        fun `skal avvise manglende påkrevd kolonne`() {
            // Arrange
            val rad: Tabellrad = emptyMap()

            // Act & Assert
            assertThatThrownBy { parseLong(Domenebegrep.BEHANDLING_ID, rad) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Fant ingen verdi for BehandlingId")
        }

        @Test
        fun `skal avvise ugyldig utfylt tall`() {
            // Arrange
            val rad: Tabellrad = mapOf("BehandlingId" to "ikke et tall")

            // Act & Assert
            assertThatThrownBy { parseLong(Domenebegrep.BEHANDLING_ID, rad) }
                .isInstanceOf(NumberFormatException::class.java)
        }
    }

    @Nested
    inner class ValgfrieFelt {
        @Test
        fun `skal returnere null for manglende kolonne og nullverdi`() {
            // Arrange
            val rader: List<Tabellrad> = listOf(emptyMap(), mapOf("BehandlingId" to null, "Status" to null))

            rader.forEach { rad ->
                // Act & Assert
                assertThat(parseValgfriString(Domenebegrep.STATUS, rad)).isNull()
                assertThat(parseValgfriLong(Domenebegrep.BEHANDLING_ID, rad)).isNull()
                assertThat(parseValgfriEnum<Domenebegrep>(Domenebegrep.STATUS, rad)).isNull()
                assertThat(parseValgfriStringList(Domenebegrep.STATUS, rad)).isEmpty()
            }
        }

        @Test
        fun `skal bevare tom streng for valgfritt tekstfelt`() {
            // Arrange
            val rad: Tabellrad = mapOf("Status" to "")

            // Act
            val status = parseValgfriString(Domenebegrep.STATUS, rad)

            // Assert
            assertThat(status).isEmpty()
        }

        @ParameterizedTest
        @NullAndEmptySource
        fun `skal tillate åpne perioder med null og tom streng`(celleverdi: String?) {
            // Arrange
            val rad: Tabellrad = mapOf("Fra dato" to "01.01.2026", "Til dato" to celleverdi, "Fra måned" to "01.2026", "Til måned" to celleverdi)

            // Act & Assert
            assertThat(parseDato(Domenebegrep.FRA_DATO, rad)).isEqualTo(LocalDate.of(2026, 1, 1))
            assertThat(parseValgfriDato(Domenebegrep.TIL_DATO, rad)).isNull()
            assertThat(parseÅrMåned(Domenebegrep.FRA_MÅNED, rad)).isEqualTo(YearMonth.of(2026, 1))
            assertThat(parseValgfriÅrMåned(Domenebegrep.TIL_MÅNED, rad)).isNull()
        }

        @Test
        fun `skal avvise ugyldig utfylt enum`() {
            // Arrange
            val rad: Tabellrad = mapOf("Status" to "ikke en enum")

            // Act & Assert
            assertThatThrownBy { parseValgfriEnum<Domenebegrep>(Domenebegrep.STATUS, rad) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }
}
