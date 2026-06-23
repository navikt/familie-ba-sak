package no.nav.familie.ba.sak.kjerne.eøs.sats

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.unmockkObject
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.Intervall
import no.nav.familie.ba.sak.kjerne.eøs.sats.EøsSatsService.finnGjeldendeSatsForLand
import no.nav.familie.ba.sak.kjerne.eøs.sats.EøsSatsService.hentSisteSatsForLand
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.YearMonth

class EøsSatsServiceTest {
    private val polskSats2024 =
        EøsSats(
            land = "PL",
            valuta = "PLN",
            intervall = Intervall.MÅNEDLIG,
            beløp = BigDecimal("800"),
            fom = YearMonth.of(2024, 1),
            tom = YearMonth.of(2024, 12),
        )

    private val polskSats2025 =
        EøsSats(
            land = "PL",
            valuta = "PLN",
            intervall = Intervall.MÅNEDLIG,
            beløp = BigDecimal("900"),
            fom = YearMonth.of(2025, 1),
            tom = null, // løpende
        )

    @BeforeEach
    fun setup() {
        mockkObject(EøsSatserPolen)
        every { EøsSatserPolen.satser } returns listOf(polskSats2024, polskSats2025)

        mockkObject(EøsSatsService)
        every { EøsSatsService.satser } returns listOf(EøsSatserPolen)
    }

    @AfterEach
    fun teardown() {
        unmockkAll()
    }

    @Nested
    inner class EøsSatsErGyldigForMåned {
        @Test
        fun `Sats med fom og tom - gyldig for måned inni perioden`() {
            assertThat(polskSats2024.erGyldigForMåned(YearMonth.of(2024, 6))).isTrue()
        }

        @Test
        fun `Sats med fom og tom - gyldig for første måned`() {
            assertThat(polskSats2024.erGyldigForMåned(YearMonth.of(2024, 1))).isTrue()
        }

        @Test
        fun `Sats med fom og tom - gyldig for siste måned`() {
            assertThat(polskSats2024.erGyldigForMåned(YearMonth.of(2024, 12))).isTrue()
        }

        @Test
        fun `Sats med fom og tom - ikke gyldig for måned etter tom`() {
            assertThat(polskSats2024.erGyldigForMåned(YearMonth.of(2025, 1))).isFalse()
        }

        @Test
        fun `Sats med fom og tom - ikke gyldig for måned før fom`() {
            assertThat(polskSats2024.erGyldigForMåned(YearMonth.of(2023, 12))).isFalse()
        }

        @Test
        fun `Løpende sats (tom = null) - gyldig for måned etter fom`() {
            assertThat(polskSats2025.erGyldigForMåned(YearMonth.of(2026, 6))).isTrue()
        }

        @Test
        fun `Løpende sats (tom = null) - ikke gyldig for måned før fom`() {
            assertThat(polskSats2025.erGyldigForMåned(YearMonth.of(2024, 12))).isFalse()
        }
    }

    @Nested
    inner class FinnGjeldendeSatsForLand {
        @Test
        fun `Returnerer korrekt sats for måned inni gyldig periode`() {
            assertThat(finnGjeldendeSatsForLand("PL", YearMonth.of(2024, 6))).isEqualTo(polskSats2024)
        }

        @Test
        fun `Returnerer løpende sats for måned etter siste fom`() {
            // Act
            val resultat = finnGjeldendeSatsForLand("PL", YearMonth.of(2025, 6))

            // Assert
            assertThat(resultat).isEqualTo(polskSats2025)
            assertThat(resultat?.tom).isNull()
        }

        @Test
        fun `Returnerer null for måned før noen satser`() {
            assertThat(finnGjeldendeSatsForLand("PL", YearMonth.of(2023, 12))).isNull()
        }

        @Test
        fun `Returnerer null når registeret er tomt`() {
            // Arrange
            every { EøsSatserPolen.satser } returns emptyList()

            // Act & Assert
            assertThat(finnGjeldendeSatsForLand("PL")).isNull()
        }
    }

    @Nested
    inner class HentSisteSatsForLand {
        @Test
        fun `Returnerer satsen med høyest fom for landet`() {
            // Act
            val resultat = hentSisteSatsForLand("PL")

            // Assert
            assertThat(resultat.beløp).isEqualTo(BigDecimal("900"))
            assertThat(resultat.fom).isEqualTo(YearMonth.of(2025, 1))
        }

        @Test
        fun `Returnerer eneste sats dersom kun én er registrert for landet`() {
            // Arrange
            every { EøsSatserPolen.satser } returns listOf(polskSats2024)

            // Act & Assert
            assertThat(hentSisteSatsForLand("PL")).isEqualTo(polskSats2024)
        }

        @Test
        fun `Kaster Feil dersom ingen sats er registrert for landet`() {
            // Arrange
            every { EøsSatserPolen.satser } returns emptyList()

            // Act & Assert
            assertThatThrownBy { hentSisteSatsForLand("PL") }.isInstanceOf(Feil::class.java)
        }
    }

    @Nested
    inner class ProduksjonsSatserErKonsistente {
        @BeforeEach
        fun setup() {
            unmockkObject(EøsSatsService)
            unmockkObject(EøsSatserPolen)
        }

        @Test
        fun `Ingen land har overlappende gyldighetsperioder`() {
            EøsSatsService.satser.forEach { (land, satser) ->
                satser.sortedBy { it.fom }.zipWithNext { nåværende, neste ->
                    assertThat(nåværende.tom != null && nåværende.tom < neste.fom)
                        .withFailMessage(
                            "Satser for land $land overlapper: ${nåværende.fom.tilKortString()} – ${nåværende.tom?.tilKortString()}" +
                                " og ${neste.fom.tilKortString()} – ${neste.tom?.tilKortString()}",
                        ).isTrue()
                }
            }
        }

        @Test
        fun `Alle satser har positivt beløp`() {
            EøsSatsService.satser.forEach { (land, satser) ->
                satser.forEach { sats ->
                    assertThat(sats.beløp)
                        .withFailMessage("Sats for $land har ikke-positivt beløp: ${sats.beløp}")
                        .isGreaterThan(BigDecimal.ZERO)
                }
            }
        }

        @Test
        fun `Alle satser har tom etter eller lik fom`() {
            EøsSatsService.satser.forEach { (land, satser) ->
                satser.forEach { sats ->
                    assertThat(sats.tom == null || sats.tom >= sats.fom)
                        .withFailMessage("Sats for $land har tom ${sats.tom?.tilKortString()} før fom ${sats.fom.tilKortString()}")
                        .isTrue()
                }
            }
        }

        @Test
        fun `EøsSats-land matcher land-objektets land-felt`() {
            EøsSatsService.satser.forEach { (land, satser) ->
                satser.forEach { sats ->
                    assertThat(sats.land)
                        .withFailMessage("Sats med land '${sats.land}' ligger i listen med satser for '$land'")
                        .isEqualTo(land)
                }
            }
        }
    }
}
