package no.nav.familie.ba.sak.cucumber.domeneparser

import io.cucumber.datatable.DataTable
import io.cucumber.datatable.DataTableTypeRegistry
import io.cucumber.datatable.DataTableTypeRegistryTableConverter
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.Locale

class DataTableUtilTest {
    private val tabellkonverterer = DataTableTypeRegistryTableConverter(DataTableTypeRegistry(Locale.forLanguageTag("nb")))

    @Test
    fun `skal bevare kolonnenavn og celleverdier`() {
        // Arrange
        val tabell =
            DataTable.create(
                listOf(
                    listOf("BehandlingId", "Fra dato"),
                    listOf("1", "2026-01-01"),
                    listOf("2", "2026-02-01"),
                ),
                tabellkonverterer,
            )

        // Act
        val rader = tabell.tilRader()

        // Assert
        assertThat(rader).containsExactly(
            mapOf("BehandlingId" to "1", "Fra dato" to "2026-01-01"),
            mapOf("BehandlingId" to "2", "Fra dato" to "2026-02-01"),
        )
        assertThat(parseLong(Domenebegrep.BEHANDLING_ID, rader.first())).isEqualTo(1L)
        assertThat(parseValgfriDato(Domenebegrep.FRA_DATO, rader.first())).isEqualTo(LocalDate.of(2026, 1, 1))
    }

    @Test
    fun `skal bevare nullverdier i valgfrie celler`() {
        // Arrange
        val tabell =
            DataTable.create(
                listOf(
                    listOf("BehandlingId", "Fra dato", "Behandlingsstatus"),
                    listOf("1", null, null),
                ),
                tabellkonverterer,
            )

        // Act
        val rad = tabell.tilRader().single()

        // Assert
        assertThat(rad).containsEntry("Fra dato", null).containsEntry("Behandlingsstatus", null)
        assertThat(parseValgfriDato(Domenebegrep.FRA_DATO, rad)).isNull()
        assertThat(parseValgfriString(Domenebegrep.BEHANDLINGSSTATUS, rad)).isNull()
        assertThat(parseEnumListe<Domenebegrep>(Domenebegrep.BEHANDLINGSSTATUS, rad)).isEmpty()
        assertThat(parseValgfriBoolean(Domenebegrep.BEHANDLINGSSTATUS, rad)).isNull()
    }

    @Test
    fun `skal avvise manglende kolonnenavn`() {
        // Arrange
        val tabell =
            DataTable.create(
                listOf(listOf<String?>(null), listOf("1")),
                tabellkonverterer,
            )

        // Act & Assert
        assertThatThrownBy { tabell.tilRader() }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Cucumber-tabellen mangler kolonnenavn")
    }

    @Test
    fun `skal fortsatt avvise manglende obligatorisk verdi`() {
        // Arrange
        val tabell =
            DataTable.create(
                listOf(listOf("BehandlingId"), listOf<String?>(null)),
                tabellkonverterer,
            )

        // Act
        val rad = tabell.tilRader().single()

        // Assert
        assertThatThrownBy { parseLong(Domenebegrep.BEHANDLING_ID, rad) }
            .isInstanceOf(RuntimeException::class.java)
            .hasMessage("Fant ingen verdi for BehandlingId")
    }

    @Test
    fun `skal returnere tom liste for tom tabell`() {
        // Arrange
        val tabell = DataTable.create(emptyList(), tabellkonverterer)

        // Act
        val rader = tabell.tilRader()

        // Assert
        assertThat(rader).isEmpty()
    }
}
