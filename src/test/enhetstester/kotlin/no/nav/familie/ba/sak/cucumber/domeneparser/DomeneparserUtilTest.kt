package no.nav.familie.ba.sak.cucumber.domeneparser

import io.cucumber.datatable.DataTable
import io.cucumber.datatable.DataTableTypeRegistry
import io.cucumber.datatable.DataTableTypeRegistryTableConverter
import no.nav.familie.ba.sak.cucumber.domeneparser.DomeneparserUtil.groupByBehandlingId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource
import java.util.Locale

class DomeneparserUtilTest {
    @Test
    fun `skal bevare celleverdier og rekkefølgen på radene`() {
        // Arrange
        val tabell =
            lagTabell(
                listOf("BehandlingId", "Fra dato", "Til dato", "Begrunnelse"),
                listOf("1", "01.01.2026", null, ""),
                listOf("2", null, "31.12.2026", " Begrunnelse med mellomrom "),
            )

        // Act
        val rader = tabell.tilTabellrader()

        // Assert
        assertThat(rader).containsExactly(
            mapOf("BehandlingId" to "1", "Fra dato" to "01.01.2026", "Til dato" to null, "Begrunnelse" to ""),
            mapOf("BehandlingId" to "2", "Fra dato" to null, "Til dato" to "31.12.2026", "Begrunnelse" to " Begrunnelse med mellomrom "),
        )
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = [" "])
    fun `skal avvise manglende kolonnenavn`(kolonnenavn: String?) {
        // Arrange
        val tabell = lagTabell(listOf("BehandlingId", kolonnenavn), listOf("1", null))

        // Act & Assert
        assertThatThrownBy { tabell.tilTabellrader() }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Kolonne 2 mangler navn i Cucumber-tabellen")
    }

    @Test
    fun `skal avvise manglende kolonnenavn også uten datarader`() {
        // Arrange
        val tabell = lagTabell(listOf("BehandlingId", null))

        // Act & Assert
        assertThatThrownBy { tabell.tilTabellrader() }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Kolonne 2 mangler navn i Cucumber-tabellen")
    }

    @Test
    fun `skal returnere tom liste for tabell uten datarader`() {
        // Arrange
        val tabell = lagTabell(listOf("BehandlingId", "Til dato"))

        // Act
        val rader = tabell.tilTabellrader()

        // Assert
        assertThat(rader).isEmpty()
    }

    @Test
    fun `skal returnere tom liste for tom tabell`() {
        // Arrange
        val tabell = lagTabell()

        // Act
        val rader = tabell.tilTabellrader()

        // Assert
        assertThat(rader).isEmpty()
    }

    @Test
    fun `skal gruppere rader med nullverdier etter behandling`() {
        // Arrange
        val tabell =
            lagTabell(
                listOf("BehandlingId", "Til dato"),
                listOf("1", null),
                listOf("2", "31.12.2026"),
                listOf("1", "30.06.2026"),
            )

        // Act
        val raderPerBehandling = tabell.groupByBehandlingId()

        // Assert
        assertThat(raderPerBehandling.getValue(1L)).containsExactly(
            mapOf("BehandlingId" to "1", "Til dato" to null),
            mapOf("BehandlingId" to "1", "Til dato" to "30.06.2026"),
        )
        assertThat(raderPerBehandling.getValue(2L)).containsExactly(mapOf("BehandlingId" to "2", "Til dato" to "31.12.2026"))
    }

    private fun lagTabell(vararg rader: List<String?>): DataTable = DataTable.create(rader.toList(), DataTableTypeRegistryTableConverter(DataTableTypeRegistry(Locale.forLanguageTag("nb-NO"))))
}
