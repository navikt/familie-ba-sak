package no.nav.familie.ba.sak.kjerne.porteføljejustering

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.BarnetrygdEnhet.OSLO
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.BarnetrygdEnhet.VADSØ
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class MappeIdMappingTest {
    @ParameterizedTest
    @CsvSource(
        "100027793, 100012753",
        "100027794, 100012782",
        "100027795, 100012754",
        "100027796, 100012783",
        "100027797, 100012755",
        "100027798, 100012785",
        "100033711, 100033712",
        "100034010, 100033910",
        "100034011, 100032451",
        "100033711, 100033712",
    )
    fun `skal returnere korrekt mappe id for Oslo når mappe id fra Steinkjer finnes i mapping`(
        mappeIdSteinkjer: Long,
        forventetMappeIdOslo: Long,
    ) {
        // Act
        val result = hentMappeIdHosOsloEllerVadsøSomTilsvarerMappeISteinkjer(mappeIdSteinkjer, OSLO.enhetsnummer)

        // Assert
        assertThat(result).isEqualTo(forventetMappeIdOslo)
    }

    @ParameterizedTest
    @CsvSource(
        "100027793, 100012691",
        "100027794, 100012692",
        "100027795, 100012693",
        "100027796, 100012721",
        "100027797, 100012694",
        "100027798, 100012695",
        "100033711, 100033731",
        "100034010, 100032890",
        "100034011, 100030295",
        "100033711, 100033731",
    )
    fun `skal returnere korrekt mappe id for Vadsø når mappe id fra Steinkjer finnes i mapping`(
        mappeIdSteinkjer: Long,
        forventetMappeIdVadsø: Long,
    ) {
        // Act
        val result = hentMappeIdHosOsloEllerVadsøSomTilsvarerMappeISteinkjer(mappeIdSteinkjer, VADSØ.enhetsnummer)

        // Assert
        assertThat(result).isEqualTo(forventetMappeIdVadsø)
    }

    @Test
    fun `skal returnere null når mappe id fra Steinkjer er null`() {
        // Act
        val result = hentMappeIdHosOsloEllerVadsøSomTilsvarerMappeISteinkjer(null, OSLO.enhetsnummer)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `skal kaste Feil når mappe id fra Steinkjer ikke finnes i mapping`() {
        // Arrange
        val ugyldigMappeIdSteinkjer = 999999L

        // Act & Assert
        val feil =
            assertThrows<Feil> {
                hentMappeIdHosOsloEllerVadsøSomTilsvarerMappeISteinkjer(ugyldigMappeIdSteinkjer, OSLO.enhetsnummer)
            }

        // Assert
        assertThat(feil.message).contains("Finner ikke mappe id $ugyldigMappeIdSteinkjer i mapping")
    }

    @Test
    fun `skal kaste Feil når enhetsnummer ikke finnes i mapping`() {
        // Arrange
        val mappeIdSteinkjer = 100027793L
        val ugyldigEnhetsnummer = "9999"

        // Act & Assert
        val feil =
            assertThrows<Feil> {
                hentMappeIdHosOsloEllerVadsøSomTilsvarerMappeISteinkjer(mappeIdSteinkjer, ugyldigEnhetsnummer)
            }

        // Assert
        assertThat(feil.message).contains("Enhet $ugyldigEnhetsnummer finnes ikke i mapping")
    }

    @Test
    fun `skal kaste Feil når enhetsnummer er tom string`() {
        // Arrange
        val mappeIdSteinkjer = 100027793L
        val tomEnhetsnummer = ""

        // Act & Assert
        val feil =
            assertThrows<Feil> {
                hentMappeIdHosOsloEllerVadsøSomTilsvarerMappeISteinkjer(mappeIdSteinkjer, tomEnhetsnummer)
            }

        // Assert
        assertThat(feil.message).contains("Enhet $tomEnhetsnummer finnes ikke i mapping")
    }
}
