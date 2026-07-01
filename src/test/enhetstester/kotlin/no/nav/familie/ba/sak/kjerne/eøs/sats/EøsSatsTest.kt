package no.nav.familie.ba.sak.kjerne.eøs.sats

import no.nav.familie.ba.sak.datagenerator.lagAktør
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.Intervall
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.filtrerErUtfylt
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.YearMonth

class EøsSatsTest {
    private val eøsSats =
        EøsSats(
            land = "PL",
            valuta = "PLN",
            beløp = BigDecimal("1200"),
            fom = YearMonth.of(2026, 1),
            tom = YearMonth.of(2026, 6),
            intervall = Intervall.MÅNEDLIG,
        )

    @Nested
    inner class Overlapper {
        @Test
        fun `returnerer true når utenladsk periodebeløp har åpen sluttdato`() {
            // Arrange
            val utenladskPeriodebeløp =
                lagUtenlandskPeriodebeløp(tom = null)
                    .tilUtfyltUtenlandskPeriodebeløp()

            // Act & Assert
            assertThat(utenladskPeriodebeløp.overlapper(eøsSats)).isTrue()
        }

        @Test
        fun `returnerer true når periodene akkurat tangerer hverandre`() {
            // Arrange
            val utenladskPeriodebeløp =
                lagUtenlandskPeriodebeløp(fom = YearMonth.of(2025, 1), tom = eøsSats.fom)
                    .tilUtfyltUtenlandskPeriodebeløp()

            // Act & Assert
            assertThat(utenladskPeriodebeløp.overlapper(eøsSats)).isTrue()
        }

        @Test
        fun `returnerer false når utenladsk periodebeløp avsluttes før satsens fom`() {
            // Arrange
            val utenladskPeriodebeløp =
                lagUtenlandskPeriodebeløp(fom = YearMonth.of(2024, 1), tom = eøsSats.fom.minusMonths(1))
                    .tilUtfyltUtenlandskPeriodebeløp()

            // Act & Assert
            assertThat(utenladskPeriodebeløp.overlapper(eøsSats)).isFalse()
        }

        @Test
        fun `returnerer false når utenladsk periodebeløp starter etter satsens tom`() {
            // Arrange
            val utenladskPeriodebeløp =
                lagUtenlandskPeriodebeløp(fom = eøsSats.tom!!.plusMonths(1), tom = null)
                    .tilUtfyltUtenlandskPeriodebeløp()

            // Act & Assert
            assertThat(utenladskPeriodebeløp.overlapper(eøsSats)).isFalse()
        }
    }

    @Nested
    inner class FiltrerErRelevantForSats {
        @Test
        fun `filtrerer bort utenladsk periodebeløp som ikke er utfylt`() {
            // Arrange
            val ikkeUtfyltUtenlandskPeriodebeløp = lagUtenlandskPeriodebeløp(beløp = null)

            // Act
            val relevante =
                listOf(ikkeUtfyltUtenlandskPeriodebeløp)
                    .filtrerErRelevantForSats(eøsSats)

            // Assert
            assertThat(relevante).isEmpty()
        }

        @Test
        fun `filtrerer bort utenladsk periodebeløp for annet land`() {
            // Arrange
            val utenlandskPeriodebeløpMedAnnetLand = lagUtenlandskPeriodebeløp(utbetalingsland = "SE")

            // Act
            val relevante =
                listOf(utenlandskPeriodebeløpMedAnnetLand)
                    .filtrerErRelevantForSats(eøsSats)

            // Assert
            assertThat(relevante).isEmpty()
        }

        @Test
        fun `filtrerer bort utenladsk periodebeløp som ikke overlapper satsen`() {
            // Arrange
            val utenlandskPeriodebeløpSomIkkeOverlapper =
                lagUtenlandskPeriodebeløp(fom = YearMonth.of(2024, 1), tom = YearMonth.of(2024, 12))

            // Act
            val relevante =
                listOf(utenlandskPeriodebeløpSomIkkeOverlapper)
                    .filtrerErRelevantForSats(eøsSats)

            // Assert
            assertThat(relevante).isEmpty()
        }

        @Test
        fun `beholder utenladsk periodebeløp som er utfylt, samme land og overlapper`() {
            // Arrange
            val utenlandskPeriodebeløp = lagUtenlandskPeriodebeløp()

            // Act
            val relevante = listOf(utenlandskPeriodebeløp).filtrerErRelevantForSats(eøsSats)

            // Assert
            assertThat(relevante).hasSize(1)
        }
    }

    private fun lagUtenlandskPeriodebeløp(
        fom: YearMonth? = YearMonth.of(2025, 1),
        tom: YearMonth? = null,
        utbetalingsland: String? = "PL",
        beløp: BigDecimal? = BigDecimal("1000"),
    ) = UtenlandskPeriodebeløp(
        fom = fom,
        tom = tom,
        barnAktører = setOf(lagAktør(randomFnr())),
        beløp = beløp,
        valutakode = "PLN",
        intervall = Intervall.MÅNEDLIG,
        utbetalingsland = utbetalingsland,
        kalkulertMånedligBeløp = beløp,
    )

    private fun UtenlandskPeriodebeløp.tilUtfyltUtenlandskPeriodebeløp() = listOf(this).filtrerErUtfylt().first()
}
