package no.nav.familie.ba.sak.kjerne.beregning.domene

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag.YtelsetypeBA
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class YtelseTypeTest {
    @Nested
    inner class TilYtelseTypeTest {
        @Test
        fun `skal mappe ORDINÆR_BARNETRYGD ytelsestype til ORDINÆR_BARNETRYGD ytelsetypeBA`() {
            // Act
            val ytelsetypeBA = YtelseType.ORDINÆR_BARNETRYGD.tilYtelseType()

            // Assert
            assertThat(ytelsetypeBA).isEqualTo(YtelsetypeBA.ORDINÆR_BARNETRYGD)
        }

        @Test
        fun `skal mappe UTVIDET_BARNETRYGD ytelsestype til UTVIDET_BARNETRYGD ytelsetypeBA`() {
            // Act
            val ytelsetypeBA = YtelseType.UTVIDET_BARNETRYGD.tilYtelseType()

            // Assert
            assertThat(ytelsetypeBA).isEqualTo(YtelsetypeBA.UTVIDET_BARNETRYGD)
        }

        @Test
        fun `skal mappe SMÅBARNSTILLEGG ytelsestype til SMÅBARNSTILLEGG ytelsetypeBA`() {
            // Act
            val ytelsetypeBA = YtelseType.SMÅBARNSTILLEGG.tilYtelseType()

            // Assert
            assertThat(ytelsetypeBA).isEqualTo(YtelsetypeBA.SMÅBARNSTILLEGG)
        }
    }

    @Nested
    inner class TilSatsTypeTest {
        @Test
        fun `tilSatsType for ORDINÆR_BARNETRYGD for en som er under 6 år i august 2024 skal returnere TILLEGGS_ORBA og ORBA`() {
            // Arrange
            val person = lagPerson(fødselsdato = LocalDate.of(2024, 8, 1).minusYears(6).plusMonths(1))
            val ytelseDatoFom = YearMonth.of(2024, 8)

            // Act
            val result = YtelseType.ORDINÆR_BARNETRYGD.tilSatsType(person, ytelseDatoFom, TIDENES_ENDE.toYearMonth())

            // Assert
            assertThat(result).isEqualTo(setOf(SatsType.TILLEGG_ORBA, SatsType.ORBA))
        }

        @Test
        fun `tilSatsType for ORDINÆR_BARNETRYGD for en som er under 6 år i september 2024 skal returnere ORBA`() {
            // Arrange
            val person = lagPerson(fødselsdato = LocalDate.of(2024, 9, 1).minusYears(6).plusMonths(1))
            val ytelseDatoFom = YearMonth.of(2024, 9)

            // Act
            val result = YtelseType.ORDINÆR_BARNETRYGD.tilSatsType(person, ytelseDatoFom, TIDENES_ENDE.toYearMonth()).single()

            // Assert
            assertThat(result).isEqualTo(SatsType.ORBA)
        }

        @Test
        fun `tilSatsType for ORDINÆR_BARNETRYGD etter 6 år`() {
            // Arrange
            val person = mockk<Person>()
            val ytelseDatoFom = YearMonth.of(2026, 1)
            every { person.hentSeksårsdag() } returns LocalDate.of(2025, 1, 1)

            // Act
            val result = YtelseType.ORDINÆR_BARNETRYGD.tilSatsType(person, ytelseDatoFom, TIDENES_ENDE.toYearMonth()).single()

            // Assert
            assertThat(result).isEqualTo(SatsType.ORBA)
        }

        @Test
        fun `tilSatsType for UTVIDET_BARNETRYGD`() {
            // Arrange
            val person = mockk<Person>()
            val ytelseDatoFom = YearMonth.of(2020, 1)

            // Act
            val result = YtelseType.UTVIDET_BARNETRYGD.tilSatsType(person, ytelseDatoFom, TIDENES_ENDE.toYearMonth()).single()

            // Assert
            assertThat(result).isEqualTo(SatsType.UTVIDET_BARNETRYGD)
        }

        @Test
        fun `tilSatsType for SMÅBARNSTILLEGG`() {
            // Arrange
            val person = mockk<Person>()
            val ytelseDatoFom = YearMonth.of(2020, 1)

            // Act
            val result = YtelseType.SMÅBARNSTILLEGG.tilSatsType(person, ytelseDatoFom, TIDENES_ENDE.toYearMonth()).single()

            // Assert
            assertThat(result).isEqualTo(SatsType.SMA)
        }
    }
}
