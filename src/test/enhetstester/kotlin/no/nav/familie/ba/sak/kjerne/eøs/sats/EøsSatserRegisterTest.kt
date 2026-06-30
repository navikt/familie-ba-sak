package no.nav.familie.ba.sak.kjerne.eøs.sats

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.unmockkObject
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.Intervall
import no.nav.familie.ba.sak.kjerne.eøs.sats.EøsSatserRegister.finnSatsForLandIMåned
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.YearMonth

class EøsSatserRegisterTest {
    private val forrigeSats =
        EøsSats(
            land = "PL",
            valuta = "PLN",
            intervall = Intervall.MÅNEDLIG,
            beløp = BigDecimal("800"),
            fom = YearMonth.of(2024, 1),
            tom = YearMonth.of(2024, 12),
        )

    private val nySats =
        EøsSats(
            land = "PL",
            valuta = "PLN",
            intervall = Intervall.MÅNEDLIG,
            beløp = BigDecimal("900"),
            fom = YearMonth.of(2025, 1),
            tom = null,
        )

    @BeforeEach
    fun setup() {
        mockkObject(EøsSatserPolen)
        every { EøsSatserPolen.satser } returns listOf(forrigeSats, nySats)

        mockkObject(EøsSatserRegister)
        every { EøsSatserRegister.satser } returns listOf(EøsSatserPolen)
    }

    @AfterEach
    fun teardown() {
        unmockkAll()
    }

    @Nested
    inner class EøsSatsErGyldigForMåned {
        @Test
        fun `Sats med fom og tom - gyldig for måned inni perioden`() {
            assertThat(forrigeSats.erGyldigForMåned(YearMonth.of(2024, 6))).isTrue()
        }

        @Test
        fun `Sats med fom og tom - gyldig for første måned`() {
            assertThat(forrigeSats.erGyldigForMåned(YearMonth.of(2024, 1))).isTrue()
        }

        @Test
        fun `Sats med fom og tom - gyldig for siste måned`() {
            assertThat(forrigeSats.erGyldigForMåned(YearMonth.of(2024, 12))).isTrue()
        }

        @Test
        fun `Sats med fom og tom - ikke gyldig for måned etter tom`() {
            assertThat(forrigeSats.erGyldigForMåned(YearMonth.of(2025, 1))).isFalse()
        }

        @Test
        fun `Sats med fom og tom - ikke gyldig for måned før fom`() {
            assertThat(forrigeSats.erGyldigForMåned(YearMonth.of(2023, 12))).isFalse()
        }

        @Test
        fun `Løpende sats (tom = null) - gyldig for måned etter fom`() {
            assertThat(nySats.erGyldigForMåned(YearMonth.of(2026, 6))).isTrue()
        }

        @Test
        fun `Løpende sats (tom = null) - ikke gyldig for måned før fom`() {
            assertThat(nySats.erGyldigForMåned(YearMonth.of(2024, 12))).isFalse()
        }
    }

    @Nested
    inner class FinnSatsForLandIMåned {
        @Test
        fun `Returnerer korrekt sats for måned inni gyldig periode`() {
            assertThat(finnSatsForLandIMåned("PL", YearMonth.of(2024, 6))).isEqualTo(forrigeSats)
        }

        @Test
        fun `Returnerer løpende sats for måned etter siste fom`() {
            // Act
            val resultat = finnSatsForLandIMåned("PL", YearMonth.of(2025, 6))

            // Assert
            assertThat(resultat).isEqualTo(nySats)
            assertThat(resultat?.tom).isNull()
        }

        @Test
        fun `Returnerer null for måned før noen satser`() {
            assertThat(finnSatsForLandIMåned("PL", YearMonth.of(2023, 12))).isNull()
        }

        @Test
        fun `Returnerer null når registeret er tomt`() {
            // Arrange
            every { EøsSatserPolen.satser } returns emptyList()

            // Act & Assert
            assertThat(finnSatsForLandIMåned("PL", YearMonth.now())).isNull()
        }
    }

    @Nested
    inner class ProduksjonsatserErKonsistente {
        @BeforeEach
        fun setup() {
            unmockkObject(EøsSatserRegister)
            unmockkObject(EøsSatserPolen)
        }

        @Test
        fun `Ingen land har overlappende gyldighetsperioder`() {
            EøsSatserRegister.satser.forEach { (land, satser) ->
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
            EøsSatserRegister.satser.forEach { (land, satser) ->
                satser.forEach { sats ->
                    assertThat(sats.beløp)
                        .withFailMessage("Sats for $land har ikke-positivt beløp: ${sats.beløp}")
                        .isGreaterThan(BigDecimal.ZERO)
                }
            }
        }

        @Test
        fun `Alle satser har tom etter eller lik fom`() {
            EøsSatserRegister.satser.forEach { (land, satser) ->
                satser.forEach { sats ->
                    assertThat(sats.tom == null || sats.tom >= sats.fom)
                        .withFailMessage("Sats for $land har tom ${sats.tom?.tilKortString()} før fom ${sats.fom.tilKortString()}")
                        .isTrue()
                }
            }
        }

        @Test
        fun `EøsSats-land matcher land-objektets land-felt`() {
            EøsSatserRegister.satser.forEach { (land, satser) ->
                satser.forEach { sats ->
                    assertThat(sats.land)
                        .withFailMessage("Sats med land '${sats.land}' ligger i listen med satser for '$land'")
                        .isEqualTo(land)
                }
            }
        }
    }
}
